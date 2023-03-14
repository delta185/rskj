package org.ethereum.vm.aa;

import org.bouncycastle.util.encoders.Hex;
import org.web3j.abi.datatypes.DynamicStruct;

import java.math.BigInteger;

public class AATransaction extends DynamicStruct {

    public AATransaction(byte txType, String sender, String receiver, byte[] gasLimit, BigInteger gasPrice, byte[] nonce,
                         BigInteger value, byte[] data, byte[] rawsignature) {
        super(
                new org.web3j.abi.datatypes.generated.Uint256(new BigInteger(new byte[]{txType})),
                new org.web3j.abi.datatypes.generated.Uint256(new BigInteger(Hex.decode(sender)).abs()),
                new org.web3j.abi.datatypes.generated.Uint256(new BigInteger(Hex.decode(receiver)).abs()),
                new org.web3j.abi.datatypes.generated.Uint256(new BigInteger(gasLimit)),
                new org.web3j.abi.datatypes.generated.Uint256(gasPrice),
                new org.web3j.abi.datatypes.generated.Uint256(new BigInteger(nonce)),
                new org.web3j.abi.datatypes.generated.Uint256(value),
                new org.web3j.abi.datatypes.DynamicBytes(data),
                new org.web3j.abi.datatypes.DynamicBytes(rawsignature)
        );
    }

}
