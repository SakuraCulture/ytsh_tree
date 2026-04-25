package cn.iocoder.yudao.module.ele.exception;

/**
 * 饿了么订单同步异常
 *
 * 错误码 6579，表示 Redis 连接异常或锁获取失败，导致订单无法安全写入。
 *
 * @author 优团科技数字化团队
 */
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
