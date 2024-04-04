package org.ethereum.rpc.exception;

import co.rsk.rpc.modules.eth.ProgramRevert;

import javax.annotation.Nullable;

public class RskJsonRpcRequestException extends RuntimeException {

    private final Integer code;

    private final byte[] revertData;

    protected RskJsonRpcRequestException(Integer code, byte[] revertData, String message, Exception e) {
        super(message, e);
        this.code = code;
        this.revertData = revertData;
    }

    protected RskJsonRpcRequestException(Integer code, String message, Exception e) {
        this(code, new byte[]{}, message, e);
    }

    public RskJsonRpcRequestException(Integer code, byte[] revertData, String message) {
        super(message);
        this.code = code;
        this.revertData = revertData;
    }

    public RskJsonRpcRequestException(Integer code, String message) {
        this(code, new byte[]{}, message);
    }

    public Integer getCode() {
        return code;
    }

    @Nullable
    public byte[] getRevertData() {
        return revertData;
    }

    public static RskJsonRpcRequestException transactionRevertedExecutionError(ProgramRevert programRevert) {
        byte[] revertData = programRevert.getData();
        String revertReason = programRevert.getReason();
        if (revertReason.isEmpty()) {

            return executionError("transaction reverted, no reason specified", revertData);
        }

        return executionError("revert " + revertReason, revertData);
    }

    public static RskJsonRpcRequestException transactionRevertedExecutionError() {
        return executionError("transaction reverted", null);
    }

    public static RskJsonRpcRequestException unknownError(String message) {
        return new RskJsonRpcRequestException(-32009, message);
    }

    private static RskJsonRpcRequestException executionError(String message, byte[] revertData) {
        return new RskJsonRpcRequestException(-32015, revertData, String.format("VM Exception while processing transaction: %s", message));
    }

    public static RskJsonRpcRequestException transactionError(String message) {
        return new RskJsonRpcRequestException(-32010, message);
    }

    public static RskJsonRpcRequestException invalidParamError(String message) {
        return new RskJsonRpcRequestException(-32602, message);
    }

    public static RskJsonRpcRequestException invalidParamError(String message, Exception e) {
        return new RskJsonRpcRequestException(-32602, message, e);
    }

    public static RskJsonRpcRequestException unimplemented(String message) {
        return new RskJsonRpcRequestException(-32201, message);
    }

    public static RskJsonRpcRequestException blockNotFound(String message) {
        return new RskJsonRpcRequestException(-32600, message);
    }

    public static RskJsonRpcRequestException stateNotFound(String message) {
        return new RskJsonRpcRequestException(-32600, message);
    }

    public static RskJsonRpcRequestException filterNotFound(String message) {
        return new RskJsonRpcRequestException(-32000, message);
    }
}
