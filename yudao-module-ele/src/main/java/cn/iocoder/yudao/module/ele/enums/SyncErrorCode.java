package cn.iocoder.yudao.module.ele.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SyncErrorCode {

    // ============== 拉取错误 (PULL_XXX) ==============
    PULL_API_TIMEOUT("PULL_API_TIMEOUT", "API调用超时", true),
    PULL_API_RATE_LIMIT("PULL_API_RATE_LIMIT", "API限流", true),
    PULL_PAGE_INCOMPLETE("PULL_PAGE_INCOMPLETE", "分页不完整", true),
    PULL_STATUS_MISSING("PULL_STATUS_MISSING", "状态订单缺失", true),
    PULL_NETWORK_ERROR("PULL_NETWORK_ERROR", "网络错误", true),
    PULL_AUTH_ERROR("PULL_AUTH_ERROR", "认证失败", false),
    PULL_DATA_FORMAT_ERROR("PULL_DATA_FORMAT_ERROR", "数据格式错误", false),

    // ============== 落库错误 (SAVE_XXX) ==============
    SAVE_DUPLICATE_KEY("SAVE_DUPLICATE_KEY", "重复键冲突", false),
    SAVE_DATA_TRUNCATION("SAVE_DATA_TRUNCATION", "数据截断", false),
    SAVE_CONSTRAINT_VIOLATION("SAVE_CONSTRAINT_VIOLATION", "约束违反", false),
    SAVE_TRANSACTION_TIMEOUT("SAVE_TRANSACTION_TIMEOUT", "事务超时", true),
    SAVE_CONNECTION_ERROR("SAVE_CONNECTION_ERROR", "数据库连接错误", true),
    SAVE_BATCH_ERROR("SAVE_BATCH_ERROR", "批量插入失败", true),

    // ============== 对账错误 (RECON_XXX) ==============
    RECON_DISCREPANCY("RECON_DISCREPANCY", "数据不一致", true),
    RECON_STATUS_MISMATCH("RECON_STATUS_MISMATCH", "状态分布不匹配", true),
    RECON_TIME_RANGE_ERROR("RECON_TIME_RANGE_ERROR", "时间范围异常", false),
    RECON_RETRY_EXHAUSTED("RECON_RETRY_EXHAUSTED", "重试次数用尽", false),
    RECON_API_DATA_CHANGED("RECON_API_DATA_CHANGED", "API数据变更", false);

    private final String code;
    private final String desc;
    private final boolean retryable;

    public static SyncErrorCode fromCode(String code) {
        for (SyncErrorCode errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        return null;
    }
}
