package co.rsk.peg.feeperkb;

import static co.rsk.peg.BridgeStorageIndexKey.FEE_PER_KB_ELECTION_KEY;
import static co.rsk.peg.BridgeStorageIndexKey.FEE_PER_KB_KEY;

import co.rsk.bitcoinj.core.Coin;
import co.rsk.peg.abielection.ABICallElection;
import co.rsk.peg.authorizer.AddressBasedAuthorizer;
import co.rsk.peg.BridgeSerializationUtils;

public class FeePerKbStorageProvider {

    private Coin feePerKb;
    private ABICallElection feePerKbElection;

    public FeePerKbStorageProvider() {

    }

    public Coin getFeePerKb() {
        if (feePerKb != null) {
            return feePerKb;
        }

        feePerKb = safeGetFromRepository(FEE_PER_KB_KEY, BridgeSerializationUtils::deserializeCoin);
        return feePerKb;
    }

    public void setFeePerKb(Coin feePerKb) {
        this.feePerKb = feePerKb;
    }

    public void saveFeePerKb() {
        if (feePerKb == null) {
            return;
        }

        safeSaveToRepository(FEE_PER_KB_KEY, feePerKb, BridgeSerializationUtils::serializeCoin);
    }


    /**
     * Save the fee per kb election
     */
    public void saveFeePerKbElection() {
        if (feePerKbElection == null) {
            return;
        }

        safeSaveToRepository(FEE_PER_KB_ELECTION_KEY, feePerKbElection, BridgeSerializationUtils::serializeElection);
    }

    public ABICallElection getFeePerKbElection(AddressBasedAuthorizer authorizer) {
        if (feePerKbElection != null) {
            return feePerKbElection;
        }

        feePerKbElection = safeGetFromRepository(FEE_PER_KB_ELECTION_KEY, data -> BridgeSerializationUtils.deserializeElection(data, authorizer));
        return feePerKbElection;
    }

}
