/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
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
package co.rsk.peg;

import co.rsk.bitcoinj.core.Address;
import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.BtcTransaction;
import co.rsk.bitcoinj.core.Coin;
import co.rsk.bitcoinj.core.Context;
import co.rsk.bitcoinj.core.NetworkParameters;
import co.rsk.bitcoinj.core.PartialMerkleTree;
import co.rsk.bitcoinj.core.ScriptException;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.TransactionInput;
import co.rsk.bitcoinj.core.UTXO;
import co.rsk.bitcoinj.core.Utils;
import co.rsk.bitcoinj.core.VerificationException;
import co.rsk.bitcoinj.crypto.TransactionSignature;
import co.rsk.bitcoinj.script.RedeemScriptParser;
import co.rsk.bitcoinj.script.RedeemScriptParser.MultiSigType;
import co.rsk.bitcoinj.script.RedeemScriptParserFactory;
import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.script.ScriptBuilder;
import co.rsk.bitcoinj.script.ScriptChunk;
import co.rsk.bitcoinj.wallet.Wallet;
import co.rsk.config.BridgeConstants;
import co.rsk.core.RskAddress;
import co.rsk.peg.bitcoin.BitcoinUtils;
import co.rsk.peg.bitcoin.RskAllowUnconfirmedCoinSelector;
import co.rsk.peg.btcLockSender.BtcLockSender.TxSenderAddressType;
import co.rsk.peg.flyover.FlyoverTxResponseCodes;
import co.rsk.peg.utils.BtcTransactionFormatUtils;
import org.ethereum.config.Constants;
import org.ethereum.config.blockchain.upgrades.ActivationConfig;
import org.ethereum.config.blockchain.upgrades.ConsensusRule;
import org.ethereum.core.SignatureCache;
import org.ethereum.core.Transaction;
import org.ethereum.util.ByteUtil;
import org.ethereum.vm.PrecompiledContracts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.ethereum.config.blockchain.upgrades.ConsensusRule.RSKIP284;
import static org.ethereum.config.blockchain.upgrades.ConsensusRule.RSKIP293;

/**
 * @author Oscar Guindzberg
 */
public class BridgeUtils {

    private static final Logger logger = LoggerFactory.getLogger("BridgeUtils");

    private BridgeUtils() {}

    public static Wallet getFederationNoSpendWallet(
        Context btcContext,
        Federation federation,
        boolean isFlyoverCompatible,
        BridgeStorageProvider storageProvider
    ) {
        return getFederationsNoSpendWallet(
            btcContext,
            Collections.singletonList(federation),
            isFlyoverCompatible,
            storageProvider
        );
    }

    public static Wallet getFederationsNoSpendWallet(
        Context btcContext,
        List<Federation> federations,
        boolean isFlyoverCompatible,
        BridgeStorageProvider storageProvider
    ) {
        Wallet wallet;
        if (isFlyoverCompatible) {
            wallet = new FlyoverCompatibleBtcWalletWithStorage(btcContext, federations, storageProvider);
        } else {
            wallet = new BridgeBtcWallet(btcContext, federations);
        }

        federations.forEach(federation ->
            wallet.addWatchedAddress(
                federation.getAddress(),
                federation.getCreationTime().toEpochMilli()
            )
        );

        return wallet;
    }

    public static Wallet getFederationSpendWallet(
        Context btcContext,
        Federation federation,
        List<UTXO> utxos,
        boolean isFlyoverCompatible,
        BridgeStorageProvider storageProvider
    ) {
        return getFederationsSpendWallet(
            btcContext,
            Collections.singletonList(federation),
            utxos,
            isFlyoverCompatible,
            storageProvider
        );
    }

    public static Wallet getFederationsSpendWallet(
        Context btcContext,
        List<Federation> federations,
        List<UTXO> utxos,
        boolean isFlyoverCompatible,
        BridgeStorageProvider storageProvider
    ) {
        Wallet wallet;
        if (isFlyoverCompatible) {
            wallet = new FlyoverCompatibleBtcWalletWithStorage(btcContext, federations, storageProvider);
        } else {
            wallet = new BridgeBtcWallet(btcContext, federations);
        }

        RskUTXOProvider utxoProvider = new RskUTXOProvider(btcContext.getParams(), utxos);
        wallet.setUTXOProvider(utxoProvider);

        federations.forEach(federation ->
            wallet.addWatchedAddress(
                federation.getAddress(),
                federation.getCreationTime().toEpochMilli()
            )
        );

        wallet.setCoinSelector(new RskAllowUnconfirmedCoinSelector());
        return wallet;
    }

    public static Coin getAmountSentToAddresses(
        ActivationConfig.ForBlock activations,
        NetworkParameters networkParameters,
        Context context,
        BtcTransaction btcTx,
        List<Address> addresses
    ) {
        if (addresses == null || addresses.isEmpty()){
            return Coin.ZERO;
        }
        if (activations.isActive(ConsensusRule.RSKIP293)){
            return getAmountSentToAddresses(
                context,
                btcTx,
                addresses
            );
        } else {
            return BridgeUtilsLegacy.getAmountSentToAddress(activations, networkParameters, btcTx, addresses.get(0));
        }
    }

    /**
     * @param minimumPegInTxValue
     * @param btcTx
     * @param addresses
     * @return true if any UTXO in the given btcTX is below the minimum pegin tx value
     */
    public static boolean isAnyUTXOAmountBelowMinimum(
            Coin minimumPegInTxValue,
            Context context,
            BtcTransaction btcTx,
            List<Address> addresses
    ){
        return isAnyUTXOAmountBelowMinimum(
                minimumPegInTxValue,
                btcTx,
                createWatchedBtcWalletFromAddresses(
                    context,
                    addresses
                )
        );
    }

    /**
     * @param minimumPegInTxValue
     * @param btcTx
     * @param wallet
     * @return true if any UTXO in the given btcTX is below the minimum pegin tx value
     */
    private static boolean isAnyUTXOAmountBelowMinimum(
            Coin minimumPegInTxValue,
            BtcTransaction btcTx,
            Wallet wallet
    ){
        return btcTx.getWalletOutputs(wallet).stream().anyMatch(transactionOutput ->
                transactionOutput.getValue().isLessThan(minimumPegInTxValue)
        );
    }

    /**
     * @param activations
     * @param bridgeConstants
     * @param btcTx
     * @param addresses
     * @return {@link FlyoverTxResponseCodes#VALID_TX} if each UTXOs sent to federation isn't less than the minimum,
     * in case any of UTXO is less than the minimum then it returns
     * {@link FlyoverTxResponseCodes#UNPROCESSABLE_TX_UTXO_AMOUNT_SENT_BELOW_MINIMUM_ERROR}.
     */
    public static FlyoverTxResponseCodes validateFlyoverPeginValue(
        ActivationConfig.ForBlock activations,
        BridgeConstants bridgeConstants,
        Context context,
        BtcTransaction btcTx,
        List<Address> addresses
    ) {
        Coin totalAmount = getAmountSentToAddresses(
            activations,
            bridgeConstants.getBtcParams(),
            context,
            btcTx,
            addresses
        );

        if (totalAmount.equals(Coin.ZERO)) {
            logger.debug("[validateFlyoverPeginValue] Amount sent can't be 0");
            return FlyoverTxResponseCodes.UNPROCESSABLE_TX_VALUE_ZERO_ERROR;
        }

        if (activations.isActive(RSKIP293)){
            Coin minimumPegInTxValue = bridgeConstants.getMinimumPeginTxValue(activations);

            if (isAnyUTXOAmountBelowMinimum(minimumPegInTxValue, context, btcTx, addresses)){
                logger.debug("[validateFlyoverPeginValue] UTXOs amount sent to federation can't be below the minimum {}.",
                    minimumPegInTxValue.value);
                return FlyoverTxResponseCodes.UNPROCESSABLE_TX_UTXO_AMOUNT_SENT_BELOW_MINIMUM_ERROR;
            }
        }
        return FlyoverTxResponseCodes.VALID_TX;
    }

    /**
     * @param context
     * @param addresses
     * @return a simple wallet instance with the give list of address added as watched addresses
     */
    private static WatchedBtcWallet createWatchedBtcWalletFromAddresses(Context context, List<Address> addresses) {
        WatchedBtcWallet wallet = new WatchedBtcWallet(context);
        long now = Utils.currentTimeMillis() / 1000L;
        wallet.addWatchedAddresses(addresses, now);
        return wallet;
    }

    /**
     *
     * @param context
     * @param btcTx
     * @param addresses
     * @return total amount sent to the given list of addresses.
     */
    private static Coin getAmountSentToAddresses(Context context, BtcTransaction btcTx, List<Address> addresses) {
        return getAmountSentToWallet(btcTx, createWatchedBtcWalletFromAddresses(context, addresses));
    }

    /**
     *
     * @param btcTx
     * @param wallet
     * @return total amount sent to a given wallet.
     */
    private static Coin getAmountSentToWallet(BtcTransaction btcTx, Wallet wallet) {
        return btcTx.getValueSentToMe(wallet);
    }

    public static List<UTXO> getUTXOsSentToAddresses(
        ActivationConfig.ForBlock activations,
        NetworkParameters networkParameters,
        Context context,
        BtcTransaction btcTx,
        List<Address> addresses

    ) {
        if (activations.isActive(ConsensusRule.RSKIP293)){
            return getUTXOsSentToAddresses(context, btcTx, addresses);
        } else {
            return BridgeUtilsLegacy.getUTXOsSentToAddress(
                activations,
                networkParameters,
                btcTx,
                addresses.get(0)
            );
        }
    }

    /**
     * @param context
     * @param btcTx
     * @param addresses
     * @return the list of UTXOs in the given btcTx sent to the given list of address
     */
    private static List<UTXO> getUTXOsSentToAddresses(Context context, BtcTransaction btcTx, List<Address> addresses) {
        Wallet wallet = BridgeUtils.createWatchedBtcWalletFromAddresses(context, addresses);
        return btcTx.getWalletOutputs(wallet).stream().map(
            txOutput -> new UTXO(
                btcTx.getHash(),
                txOutput.getIndex(),
                txOutput.getValue(),
                0,
                btcTx.isCoinBase(),
                txOutput.getScriptPubKey()
            )
        ).collect(Collectors.toList());
    }

    public static boolean scriptCorrectlySpendsTx(BtcTransaction tx, int index, Script script) {
        try {
            TransactionInput txInput = tx.getInput(index);

            // Check the input does not contain script op codes
            List<ScriptChunk> chunks = txInput.getScriptSig().getChunks();
            Iterator it = chunks.iterator();
            while(it.hasNext()) {
                ScriptChunk chunk = (ScriptChunk) it.next();
                if (chunk.isOpCode() && chunk.opcode > 96) {
                    return false;
                }
            }

            txInput.getScriptSig().correctlySpends(tx, index, script, Script.ALL_VERIFY_FLAGS);
            return true;
        } catch (ScriptException se) {
            return false;
        }
    }

    /**
     * It checks if the tx doesn't spend any of the federations' funds and if it sends more than
     * the minimum ({@see BridgeConstants::getMinimumLockTxValue}) to any of the federations
     * @param tx the BTC transaction to check
     * @param activeFederations the active federations
     * @param retiredFederationP2SHScript the retired federation P2SHScript. Could be {@code null}.
     * @param btcContext the BTC Context
     * @param minimumPegInTxValue minimum peg-in tx value allowed
     * @param activations the network HF activations configuration
     * @return true if this is a valid peg-in transaction
     */
    public static boolean isValidPegInTx(
        BtcTransaction tx,
        List<Federation> activeFederations,
        Script retiredFederationP2SHScript,
        Context btcContext,
        Coin minimumPegInTxValue,
        ActivationConfig.ForBlock activations) {

        // First, check tx is not a typical release tx (tx spending from any of the federation addresses and
        // optionally sending some change to any of the federation addresses)
        for (int i = 0; i < tx.getInputs().size(); i++) {
            final int index = i;
            if (activeFederations.stream().anyMatch(federation -> scriptCorrectlySpendsTx(tx, index, federation.getP2SHScript()))) {
                return false;
            }

            if (retiredFederationP2SHScript != null && scriptCorrectlySpendsTx(tx, index, retiredFederationP2SHScript)) {
                return false;
            }

            // Check if the registered utxo is not change from an utxo spent from either a fast bridge federation,
            // erp federation, or even a retired fast bridge or erp federation
            if (activations.isActive(ConsensusRule.RSKIP201)) {
                RedeemScriptParser redeemScriptParser = RedeemScriptParserFactory.get(tx.getInput(index).getScriptSig().getChunks());
                try {
                    // Consider transactions that have an input with a redeem script of type P2SH ERP FED
                    // to be "future transactions" that should not be pegins. These are gonna be considered pegouts.
                    // This is only for backwards compatibility reasons. As soon as RSKIP353 activates,
                    // pegins to the new federation should be valid again.
                    // There's no reason for someone to send an actual pegin of this type before the new fed is active.
                    // TODO: Remove this if block after RSKIP353 activation
                    if (!activations.isActive(ConsensusRule.RSKIP353) &&
                        (redeemScriptParser.getMultiSigType() == MultiSigType.P2SH_ERP_FED ||
                        redeemScriptParser.getMultiSigType() == MultiSigType.FAST_BRIDGE_P2SH_ERP_FED)) {
                        String message = "Tried to register a transaction with a P2SH ERP federation redeem script before RSKIP353 activation";
                        logger.warn("[isValidPegInTx] {}", message);
                        throw new ScriptException(message);
                    }
                    Script inputStandardRedeemScript = redeemScriptParser.extractStandardRedeemScript();
                    if (activeFederations.stream().anyMatch(federation -> federation.getStandardRedeemScript().equals(inputStandardRedeemScript))) {
                        return false;
                    }

                    Script outputScript = ScriptBuilder.createP2SHOutputScript(inputStandardRedeemScript);
                    if (outputScript.equals(retiredFederationP2SHScript)) {
                        return false;
                    }
                } catch (ScriptException e) {
                    // There is no redeem script, could be a peg-in from a P2PKH address
                }
            }
        }

        Wallet federationsWallet = BridgeUtils.getFederationsNoSpendWallet(
            btcContext,
            activeFederations,
            false,
            null
        );
        Coin valueSentToMe = tx.getValueSentToMe(federationsWallet);

        boolean isUTXOsOrTxAmountBelowMinimum =
            activations.isActive(RSKIP293) ? isAnyUTXOAmountBelowMinimum(
                minimumPegInTxValue,
                tx,
                federationsWallet
            ) : valueSentToMe.isLessThan(minimumPegInTxValue); // Legacy minimum validation against the total amount

        if (!isUTXOsOrTxAmountBelowMinimum) {
            return true;
        }

        logger.warn(
            activations.isActive(RSKIP293)?
                "[btctx:{}] Someone sent to the federation UTXOs amount less than {} satoshis":
                "[btctx:{}] Someone sent to the federation less than {} satoshis",
            tx.getHash(),
            minimumPegInTxValue
        );
        return false;
    }

    /**
     * It checks if the tx doesn't spend any of the federations' funds and if it sends more than
     * the minimum ({@see BridgeConstants::getMinimumLockTxValue}) to any of the federations
     * @param tx the BTC transaction to check
     * @param federation the active federation
     * @param btcContext the BTC Context
     * @param bridgeConstants the Bridge constants
     * @param activations the network HF activations configuration
     * @return true if this is a valid peg-in transaction
     */
    public static boolean isValidPegInTx(
        BtcTransaction tx,
        Federation federation,
        Context btcContext,
        BridgeConstants bridgeConstants,
        ActivationConfig.ForBlock activations) {

        return isValidPegInTx(
            tx,
            Collections.singletonList(federation),
            null,
            btcContext,
            bridgeConstants.getMinimumPeginTxValue(activations),
            activations
        );
    }

    /**
     * It checks if the tx can be processed, if it is sent from a P2PKH address or RSKIP 143 is active
     * and the sender could be obtained
     * @param txSenderAddressType sender of the transaction address type
     * @param activations to identify if certain hardfork is active or not.
     * @return true if this tx can be locked
     */
    public static boolean txIsProcessableInLegacyVersion(TxSenderAddressType txSenderAddressType, ActivationConfig.ForBlock activations) {
        //After RSKIP 143 activation, check if the tx sender could be obtained to process the tx
        return txSenderAddressType == TxSenderAddressType.P2PKH ||
            (activations.isActive(ConsensusRule.RSKIP143) && txSenderAddressType != TxSenderAddressType.UNKNOWN);
    }

    private static boolean isPegOutTx(BtcTransaction tx, Federation federation, ActivationConfig.ForBlock activations) {
        return isPegOutTx(tx, Collections.singletonList(federation), activations);
    }

    public static boolean isPegOutTx(BtcTransaction tx, List<Federation> federations, ActivationConfig.ForBlock activations) {
        return isPegOutTx(tx, activations, federations.stream().filter(Objects::nonNull).map(Federation::getStandardP2SHScript).toArray(Script[]::new));
    }

    public static boolean isPegOutTx(BtcTransaction tx, ActivationConfig.ForBlock activations, Script... p2shScript) {
        int inputsSize = tx.getInputs().size();
        for (int i = 0; i < inputsSize; i++) {
            TransactionInput txInput = tx.getInput(i);
            Optional<Script> redeemScriptOptional = BitcoinUtils.extractRedeemScriptFromInput(tx.getInput(i));
            if (!redeemScriptOptional.isPresent()) {
                continue;
            }

            Script redeemScript = redeemScriptOptional.get();
            if (activations.isActive(ConsensusRule.RSKIP201)) {
                // Extract standard redeem script since the registered utxo could be from a fast bridge or erp federation
                RedeemScriptParser redeemScriptParser = RedeemScriptParserFactory.get(txInput.getScriptSig().getChunks());
                try {
                    redeemScript = redeemScriptParser.extractStandardRedeemScript();
                } catch (ScriptException e) {
                    // There is no redeem script
                    continue;
                }
            }

            Script outputScript = ScriptBuilder.createP2SHOutputScript(redeemScript);
            if (Stream.of(p2shScript).anyMatch(federationPayScript -> federationPayScript.equals(outputScript))) {
                return true;
            }
        }

        return false;
    }

    public static boolean isMigrationTx(
        BtcTransaction btcTx,
        Federation activeFederation,
        Federation retiringFederation,
        Script retiredFederationP2SHScript,
        Context btcContext,
        BridgeConstants bridgeConstants,
        ActivationConfig.ForBlock activations) {

        if (retiredFederationP2SHScript == null && retiringFederation == null) {
            return false;
        }
        boolean moveFromRetired = retiredFederationP2SHScript != null && isPegOutTx(btcTx, activations, retiredFederationP2SHScript);
        boolean moveFromRetiring = retiringFederation != null && isPegOutTx(btcTx, retiringFederation, activations);
        boolean moveToActive = isValidPegInTx(btcTx, activeFederation, btcContext, bridgeConstants, activations);

        return (moveFromRetired || moveFromRetiring) && moveToActive;
    }

    /**
     * Return the amount of missing signatures for a tx.
     * @param btcContext Btc context
     * @param btcTx The btc tx to check
     * @return 0 if was signed by the required number of federators, amount of missing signatures otherwise
     */
    public static int countMissingSignatures(Context btcContext, BtcTransaction btcTx) {
        // Check missing signatures for only one input as it is not
        // possible for a federator to leave unsigned inputs in a tx
        Context.propagate(btcContext);
        int unsigned = 0;

        TransactionInput input = btcTx.getInput(0);
        Script scriptSig = input.getScriptSig();
        List<ScriptChunk> chunks = scriptSig.getChunks();
        Script redeemScript = new Script(chunks.get(chunks.size() - 1).data);
        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScript.getChunks());
        MultiSigType multiSigType;

        int lastChunk;

        multiSigType = parser.getMultiSigType();

        if (multiSigType == MultiSigType.STANDARD_MULTISIG ||
            multiSigType == MultiSigType.FAST_BRIDGE_MULTISIG
        ) {
            lastChunk = chunks.size() - 1;
        } else {
            lastChunk = chunks.size() - 2;
        }

        for (int i = 1; i < lastChunk; i++) {
            ScriptChunk chunk = chunks.get(i);
            if (!chunk.isOpCode() && chunk.data.length == 0) {
                unsigned++;
            }
        }
        return unsigned;
    }

    /**
     * Checks whether a btc tx has been signed by the required number of federators.
     * @param btcContext Btc context
     * @param btcTx The btc tx to check
     * @return True if was signed by the required number of federators, false otherwise
     */
    public static boolean hasEnoughSignatures(Context btcContext, BtcTransaction btcTx) {
        // When the tx is constructed OP_0 are placed where signature should go.
        // Check all OP_0 have been replaced with actual signatures in all inputs
        Context.propagate(btcContext);
        Script scriptSig;
        List<ScriptChunk> chunks;
        Script redeemScript;
        RedeemScriptParser parser;
        MultiSigType multiSigType;

        int lastChunk;
        for (TransactionInput input : btcTx.getInputs()) {
            scriptSig = input.getScriptSig();
            chunks = scriptSig.getChunks();
            redeemScript = new Script(chunks.get(chunks.size() - 1).data);
            parser = RedeemScriptParserFactory.get(redeemScript.getChunks());
            multiSigType = parser.getMultiSigType();

            if (multiSigType == MultiSigType.STANDARD_MULTISIG ||
            multiSigType == MultiSigType.FAST_BRIDGE_MULTISIG
            ) {
                lastChunk = chunks.size() - 1;
            } else {
                lastChunk = chunks.size() - 2;
            }

            for (int i = 1; i < lastChunk; i++) {
                ScriptChunk chunk = chunks.get(i);
                if (!chunk.isOpCode() && chunk.data.length == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public static Address recoverBtcAddressFromEthTransaction(org.ethereum.core.Transaction tx, NetworkParameters networkParameters) {
        org.ethereum.crypto.ECKey key = tx.getKey();
        byte[] pubKey = key.getPubKey(true);
        return BtcECKey.fromPublicOnly(pubKey).toAddress(networkParameters);
    }

    public static boolean isFreeBridgeTx(Transaction rskTx, Constants constants, ActivationConfig.ForBlock activations, SignatureCache signatureCache) {
        RskAddress receiveAddress = rskTx.getReceiveAddress();
        if (receiveAddress.equals(RskAddress.nullAddress())) {
            return false;
        }

        BridgeConstants bridgeConstants = constants.getBridgeConstants();

        // Temporary assumption: if areBridgeTxsFree() is true then the current federation
        // must be the genesis federation.
        // Once the original federation changes, txs are always paid.
        return PrecompiledContracts.BRIDGE_ADDR.equals(receiveAddress) &&
               !activations.isActive(ConsensusRule.ARE_BRIDGE_TXS_PAID) &&
               rskTx.acceptTransactionSignature(constants.getChainId()) &&
               (
                       isFromFederateMember(rskTx, bridgeConstants.getGenesisFederation(), signatureCache) ||
                       isFromFederationChangeAuthorizedSender(rskTx, bridgeConstants, signatureCache) ||
                       isFromLockWhitelistChangeAuthorizedSender(rskTx, bridgeConstants, signatureCache) ||
                       isFromFeePerKbChangeAuthorizedSender(rskTx, bridgeConstants, signatureCache)
               );
    }

    /**
     * Indicates if the provided tx was generated from a contract
     * @param rskTx
     * @return
     */
    public static boolean isContractTx(Transaction rskTx) {
        // TODO: this should be refactored to provide a more robust way of checking the transaction origin
        return rskTx.getClass() == org.ethereum.vm.program.InternalTransaction.class;
    }

    public static boolean isFromFederateMember(org.ethereum.core.Transaction rskTx, Federation federation, SignatureCache signatureCache) {
        return federation.hasMemberWithRskAddress(rskTx.getSender(signatureCache).getBytes());
    }

    public static Coin getCoinFromBigInteger(BigInteger value) throws BridgeIllegalArgumentException {
        if (value == null) {
            throw new BridgeIllegalArgumentException("value cannot be null");
        }
        try {
            return Coin.valueOf(value.longValueExact());
        } catch(ArithmeticException e) {
            throw new BridgeIllegalArgumentException(e.getMessage(), e);
        }
    }

    private static boolean isFromFederationChangeAuthorizedSender(org.ethereum.core.Transaction rskTx, BridgeConstants bridgeConfiguration, SignatureCache signatureCache) {
        AddressBasedAuthorizer authorizer = bridgeConfiguration.getFederationChangeAuthorizer();
        return authorizer.isAuthorized(rskTx, signatureCache);
    }

    private static boolean isFromLockWhitelistChangeAuthorizedSender(org.ethereum.core.Transaction rskTx, BridgeConstants bridgeConfiguration, SignatureCache signatureCache) {
        AddressBasedAuthorizer authorizer = bridgeConfiguration.getLockWhitelistChangeAuthorizer();
        return authorizer.isAuthorized(rskTx, signatureCache);
    }

    private static boolean isFromFeePerKbChangeAuthorizedSender(org.ethereum.core.Transaction rskTx, BridgeConstants bridgeConfiguration, SignatureCache signatureCache) {
        AddressBasedAuthorizer authorizer = bridgeConfiguration.getFeePerKbChangeAuthorizer();
        return authorizer.isAuthorized(rskTx, signatureCache);
    }

    public static boolean validateHeightAndConfirmations(int height, int btcBestChainHeight, int acceptableConfirmationsAmount, Sha256Hash btcTxHash) throws Exception {
        // Check there are at least N blocks on top of the supplied height
        if (height < 0) {
            throw new Exception("Height can't be lower than 0");
        }
        int confirmations = btcBestChainHeight - height + 1;
        if (confirmations < acceptableConfirmationsAmount) {
            logger.warn(
                    "Btc Tx {} at least {} confirmations are required, but there are only {} confirmations",
                    btcTxHash,
                    acceptableConfirmationsAmount,
                    confirmations
            );
            return false;
        }
        return true;
    }

    public static Sha256Hash calculateMerkleRoot(NetworkParameters networkParameters, byte[] pmtSerialized, Sha256Hash btcTxHash) throws VerificationException{
        PartialMerkleTree pmt = new PartialMerkleTree(networkParameters, pmtSerialized, 0);
        List<Sha256Hash> hashesInPmt = new ArrayList<>();
        Sha256Hash merkleRoot = pmt.getTxnHashAndMerkleRoot(hashesInPmt);
        if (!hashesInPmt.contains(btcTxHash)) {
            logger.warn("Supplied Btc Tx {} is not in the supplied partial merkle tree", btcTxHash);
            return null;
        }
        return merkleRoot;
    }

    public static void validateInputsCount(byte[] btcTxSerialized, boolean isActiveRskip) throws VerificationException.EmptyInputsOrOutputs {
        if (BtcTransactionFormatUtils.getInputsCount(btcTxSerialized) == 0) {
            if (isActiveRskip) {
                if (BtcTransactionFormatUtils.getInputsCountForSegwit(btcTxSerialized) == 0) {
                    logger.warn("Provided btc segwit tx has no inputs");
                    // this is the exception thrown by co.rsk.bitcoinj.core.BtcTransaction#verify when there are no inputs.
                    throw new VerificationException.EmptyInputsOrOutputs();
                }
            } else {
                logger.warn("Provided btc tx has no inputs ");
                // this is the exception thrown by co.rsk.bitcoinj.core.BtcTransaction#verify when there are no inputs.
                throw new VerificationException.EmptyInputsOrOutputs();
            }
        }
    }

    /**
     * Check if the p2sh multisig scriptsig of the given input was already signed by federatorPublicKey.
     * @param federatorPublicKey The key that may have been used to sign
     * @param sighash the sighash that corresponds to the input
     * @param input The input
     * @return true if the input was already signed by the specified key, false otherwise.
     */
    public static boolean isInputSignedByThisFederator(BtcECKey federatorPublicKey, Sha256Hash sighash, TransactionInput input) {
        List<ScriptChunk> chunks = input.getScriptSig().getChunks();
        for (int j = 1; j < chunks.size() - 1; j++) {
            ScriptChunk chunk = chunks.get(j);

            if (chunk.data.length == 0) {
                continue;
            }

            TransactionSignature sig2 = TransactionSignature.decodeFromBitcoin(chunk.data, false, false);

            if (federatorPublicKey.verify(sighash, sig2)) {
                return true;
            }
        }
        return false;
    }

    public static byte[] serializeBtcAddressWithVersion(ActivationConfig.ForBlock activations, Address btcAddress) {
        byte[] hash160 = btcAddress.getHash160();
        byte[] version = BigInteger.valueOf(btcAddress.getVersion()).toByteArray();
        if (activations.isActive(RSKIP284)) {
            // BigInteger adds a leading byte to indicate the sign,
            // but we need the version number to be 1 byte only.
            // Use new serialization after HF activation
            version = btcAddress.getVersion() != 0 ?
                ByteUtil.intToBytesNoLeadZeroes(btcAddress.getVersion()) :
                new byte[]{0};
        }

        byte[] btcAddressBytes = new byte[version.length + hash160.length];
        System.arraycopy(version, 0, btcAddressBytes, 0, version.length);
        System.arraycopy(hash160, 0, btcAddressBytes, version.length, hash160.length);

        return btcAddressBytes;
    }

    public static Address deserializeBtcAddressWithVersion(
        NetworkParameters networkParameters,
        ActivationConfig.ForBlock activations,
        byte[] addressBytes) throws BridgeIllegalArgumentException {

        if (!activations.isActive(ConsensusRule.RSKIP284)) {
            return BridgeUtilsLegacy.deserializeBtcAddressWithVersionLegacy(networkParameters, activations, addressBytes);
        }

        // We expect 1 byte for the address version and 20 for the script hash / pub key hash
        if (addressBytes == null || addressBytes.length != 21) {
            throw new BridgeIllegalArgumentException("Invalid address, expected 21 bytes long array");
        }

        int version = ByteUtil.byteArrayToInt(new byte[]{addressBytes[0]});

        byte[] hashBytes = new byte[20];
        System.arraycopy(addressBytes, 1, hashBytes, 0, 20);

        return new Address(networkParameters, version, hashBytes);
    }

    public static int getRegularPegoutTxSize(ActivationConfig.ForBlock activations, @Nonnull Federation federation) {
        // A regular peg-out transaction has two inputs and two outputs
        // Each input has M/N signatures and each signature is around 71 bytes long (signed sighash)
        // The outputs are composed of the scriptPubkeyHas(or publicKeyHash)
        // and the op_codes for the corresponding script
        final int INPUT_MULTIPLIER = 2;
        final int OUTPUT_MULTIPLIER = 2;

        return calculatePegoutTxSize(
            activations,
            federation,
            INPUT_MULTIPLIER,
            OUTPUT_MULTIPLIER
        );
    }

    public static int calculatePegoutTxSize(ActivationConfig.ForBlock activations, Federation federation, int inputs, int outputs) {

        if (inputs < 1 || outputs < 1) {
            throw new IllegalArgumentException("Inputs or outputs should be more than 1");
        }

        if (!activations.isActive(ConsensusRule.RSKIP271)) {
            return BridgeUtilsLegacy.calculatePegoutTxSize(activations, federation, inputs, outputs);
        }

        final int SIGNATURE_MULTIPLIER = 72;
        BtcTransaction pegoutTx = new BtcTransaction(federation.btcParams);
        for (int i = 0; i < inputs; i++) {
            pegoutTx.addInput(Sha256Hash.ZERO_HASH, 0, federation.getRedeemScript());
        }
        for (int i = 0; i < outputs; i++) {
            pegoutTx.addOutput(Coin.ZERO, federation.getAddress());
        }
        int baseSize = pegoutTx.bitcoinSerialize().length;
        int signingSize = federation.getNumberOfSignaturesRequired() * inputs * SIGNATURE_MULTIPLIER;

        return baseSize + signingSize;
    }
}
