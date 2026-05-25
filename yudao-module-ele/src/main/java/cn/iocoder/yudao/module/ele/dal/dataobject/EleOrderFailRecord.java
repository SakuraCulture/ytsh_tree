package cn.iocoder.yudao.module.ele.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ele_order_fail_record_table")
public class EleOrderFailRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String platformType;

    private Long storeId;

    private String orderId;

    private String channelOrderId;

    private String bizType;

    private String failStage;

    private String failCode;

    private String failMessage;

    /** 超时阶段：REDIS_QUERY/STORE_INFO_QUERY/API_CALL/RATE_LIMIT_WAIT/TOTAL_TIMEOUT/SINGLE_TIMEOUT */
    private String timeoutStage;

    /** 超时用时（毫秒） */
    private Long timeoutElapsedMs;

    /** 超时阈值（毫秒） */
    private Long timeoutThresholdMs;

    private String requestParam;

    private String responseContent;

    private Integer retryCount;

    private Integer maxRetryCount;

    private String processStatus;

    private String taskId;

    private String remark;

    private String platformStoreId;

    private String merchantCode;

    private String erpStoreCode;

    private String tenantId;

    private Long createTime;

    private Long updateTime;
}
