/*
 * This file is part of RskJ
 * Copyright (C) 2020 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package co.rsk.pcc;

import co.rsk.core.RskAddress;
import org.ethereum.core.AccountState;
import org.ethereum.core.Block;
import org.ethereum.core.Repository;
import org.ethereum.core.Transaction;
import org.ethereum.crypto.ECKey;
import org.ethereum.crypto.HashUtil;
import org.ethereum.crypto.Keccak256Helper;
import org.ethereum.crypto.signature.ECDSASignature;
import org.ethereum.crypto.signature.Secp256k1;
import org.ethereum.db.BlockStore;
import org.ethereum.db.ReceiptStore;
import org.ethereum.util.ByteUtil;
import org.ethereum.vm.GasCost;
import org.ethereum.vm.LogInfo;
import org.ethereum.vm.PrecompiledContracts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.List;

public class InstallCode extends PrecompiledContracts.PrecompiledContract {

    private static final Logger logger = LoggerFactory.getLogger(InstallCode.class);

    private Repository repository;

    // The base cost was chosen to be the difference between CREATE and NEW_ACCT_CALL.
    // It should be higher than 5000 (a storage cell change) and lower than 10K
    // (a storage cell creation)
    private static final long STORAGE_COST = 7000; // this is the costo of creation of code node in trie
    // This is the ECRECOVER cost
    public static final long ECDSA_PKRECOVER_COST = 3000; // This is the cost of signature verification
    public static final long BASE_COST = ECDSA_PKRECOVER_COST + STORAGE_COST;
    private static final int ARG_SIZE = 32;
    private static final int MIN_ARG_SIZE = ARG_SIZE * 4;
    private static byte[] ERROR_RETURN = new byte[1]; // 0 = error
    private static byte[] SUCCESS_RETURN = new byte[]{1};// 1 = success

    @Override
    public void init(Transaction rskTx, Block rskExecutionBlock, Repository repository, BlockStore rskBlockStore, ReceiptStore rskReceiptStore, List<LogInfo> logs) {
        this.repository = repository;
    }

    @Override
    public long getGasForData(byte[] data) {

        try {
            long cost = BASE_COST;
            // If that is not enough, we do not charge the user.
            if (data.length < MIN_ARG_SIZE) {
                throw new IllegalArgumentException("Invalid arguments.");
            }
            ByteBuffer dataBytes = ByteBuffer.wrap(data);

            byte[] address = readAddressArgument(dataBytes);
            RskAddress accountAddress = getRskAddress(address);
            AccountState state = repository.getAccountState(accountAddress);
            if (state == null) {
                cost = cost + GasCost.NEW_ACCT_CALL;
            }

            // We subsidize contracts having a delegate call to a library.
            // This is a benefit for the platform because code can be embedded into the node data.
            int codeLength = data.length - MIN_ARG_SIZE;
            if (codeLength < 64) {
                return cost;
            }
            cost += (codeLength - 64) * GasCost.CREATE_DATA;
            return cost;
        } catch (Exception any) {
            logger.warn("InstallCode failed.", any);
            return 0;
        }
    }

    @Override
    public byte[] execute(byte[] data) {
        try {
            // fail fast if not enough arguments were passed
            if (data.length < MIN_ARG_SIZE) {
                throw new IllegalArgumentException("Invalid arguments.");
            }

            ByteBuffer dataBytes = ByteBuffer.wrap(data);

            // 1st - Account address
            byte[] address = readAddressArgument(dataBytes);
            RskAddress rskAddress = getRskAddress(address);

            // Signature, 32-byte padded always
            // 2nd - signature-v
            byte[] v = readArgument(dataBytes, new byte[ARG_SIZE]);
            // 3rd - signature-r
            byte[] r = readArgument(dataBytes, new byte[ARG_SIZE]);
            // 4th - signature-s
            byte[] s = readArgument(dataBytes, new byte[ARG_SIZE]);
            checkSignatureFormat(r, s, v);

            // 5th - bytecode
            // TODO: we can check here for the hash of the code, if it doesn't match the default, we fail.
            byte[] code = readArgument(dataBytes, new byte[data.length - MIN_ARG_SIZE]);

            // Get nonce if any
            AccountState state = repository.getAccountState(rskAddress);
            byte[] nonce = state == null ? new byte[32] : ByteUtil.copyToArray(state.getNonce());


            byte[] h = getHashToSignFromCode(address, nonce, code);
            checkSignature(rskAddress, v, r, s, h);

            // if account doesn't exist, it may be the case the user only owns tokens.
            // we must create it.
            if (state == null) {
                state = repository.createAccount(rskAddress);
            }

            state.smarty();
            repository.updateAccountState(rskAddress, state);

            // Now let the user replace the code if existent.
            if (!repository.isContract(rskAddress)) {
                repository.setupContract(rskAddress);
            }

            repository.saveCode(rskAddress, code);

        } catch (Exception any) {
            logger.warn("InstallCode failed.", any);
            return ERROR_RETURN;
        }
        return SUCCESS_RETURN;
    }

    private void checkSignature(RskAddress rskAddress, byte[] v, byte[] r, byte[] s, byte[] h) throws SignatureException {
        ECDSASignature signature = ECDSASignature.fromComponents(r, s, v[31]);

        ECKey key = Secp256k1.getInstance().signatureToKey(h, signature);
        // now we verify that the account and address recovered are the same
        // In the future we could verify the signature instead
        // of pubkey recovery. It's faster.
        if (!Arrays.equals(key.getAddress(), rskAddress.getBytes())) {
            throw new IllegalArgumentException("Invalid signature recovered.");
        }
    }

    static public byte[] getHashToSignFromCode(byte[] account, byte[] nonce, byte[] code) {
        // first hash the code
        byte[] codeHash = Keccak256Helper.keccak256(code);
        byte[] message = buildMessageToSign(account, nonce, codeHash);
        byte[] h = Keccak256Helper.keccak256(message);

        // 0x19 = 25, length should be an ascii decimals, message - original
        String prefix = (char) 25 + "Ethereum Signed Message:\n" + h.length;

        byte[] messageHash = HashUtil.keccak256(ByteUtil.merge(
                prefix.getBytes(StandardCharsets.UTF_8),
                h
        ));
        return messageHash;
    }

    static public byte[] buildMessageToSign(byte[] account, byte[] nonce, byte[] codeHash) {
        assert (account.length == 32);
        assert (nonce.length == 32);
        assert (codeHash.length == 32);
        byte[] message = new byte[32 + 32 + 32]; // account + nonce + codehash
        System.arraycopy(account, 0, message, 0, 32);
        System.arraycopy(nonce, 0, message, 32, 32);
        System.arraycopy(codeHash, 0, message, 64, 32);
        return message;
    }

    public static void checkSignatureFormat(byte[] rBytes, byte[] sBytes, byte[] vBytes) {
        byte v = vBytes[vBytes.length - 1];
        BigInteger r = new BigInteger(1, rBytes);
        BigInteger s = new BigInteger(1, sBytes);

        if (!ECDSASignature.validateComponents(r, s, v)) {
            throw new IllegalArgumentException("Invalid signature.");
        }
    }

    private RskAddress getRskAddress(byte[] address) {
        return new RskAddress(Arrays.copyOfRange(address, 12, 32));
    }

    private byte[] readAddressArgument(ByteBuffer dataBytes) {
        byte[] address = new byte[ARG_SIZE];
        readArgument(dataBytes, address);
        // We validate that first 12 bytes must be zero. We do not allow
        // other kind of addresses now but we let this happen in the future
        if (!Arrays.equals(new byte[12], Arrays.copyOfRange(address, 0, 12))) {
            throw new IllegalArgumentException("Address not padded with 12 bytes of 0");
        }
        return address;
    }

    private byte[] readArgument(ByteBuffer dataBytes, byte[] argument) {
        dataBytes.get(argument);
        return argument;
    }
}

