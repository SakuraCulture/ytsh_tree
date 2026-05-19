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
