package cn.iocoder.yudao.module.ele.exception;


public class EleOrderSyncException extends RuntimeException {

    public static final int ERROR_CODE = 6579;

    private final int errorCode;

    public EleOrderSyncException(String message) {
        super(message);
        this.errorCode = ERROR_CODE;
    }

    public EleOrderSyncException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ERROR_CODE;
    }

    public int getErrorCode() {
        return errorCode;
    }

}
