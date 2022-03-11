package co.rsk.net.handler.quota;

import co.rsk.util.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxQuota {

    private static final Logger logger = LoggerFactory.getLogger(TxQuota.class);

    private final String txHash;
    private final TimeProvider timeProvider;
    private long timestamp;
    private double availableVirtualGas;

    public static TxQuota createNew(String txHash, long initialQuota, TimeProvider timeProvider) {
        return new TxQuota(txHash, initialQuota, timeProvider);
    }

    private TxQuota(String txHash, long availableVirtualGas, TimeProvider timeProvider) {
        this.txHash = txHash;
        this.timeProvider = timeProvider;
        this.timestamp = this.timeProvider.currentTimeMillis();
        this.availableVirtualGas = availableVirtualGas;

        logger.trace("quota created for tx [{}] with value [{}]", this.txHash, this.availableVirtualGas);
    }

    public boolean acceptVirtualGasConsumption(double virtualGasToConsume) {
        if (this.availableVirtualGas < virtualGasToConsume) {
            return false;
        }

        this.availableVirtualGas -= virtualGasToConsume;
        return true;
    }

    public void refresh(long maxGasPerSecond, long maxQuota) {
        long currentTimestamp = this.timeProvider.currentTimeMillis();

        double timeDiffSeconds = (currentTimestamp - this.timestamp) / 1000d;
        double addToQuota = timeDiffSeconds * maxGasPerSecond;
        this.timestamp = currentTimestamp;
        this.availableVirtualGas = Math.min(maxQuota, this.availableVirtualGas + addToQuota);

        logger.trace("quota refreshed for tx [{}] to [{}] (addToQuota [{}])", this.txHash, this.availableVirtualGas, addToQuota);
    }
}
