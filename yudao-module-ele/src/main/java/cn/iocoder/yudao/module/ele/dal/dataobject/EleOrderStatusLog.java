package cn.iocoder.yudao.module.ele.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ele_order_status_log_table")
public class EleOrderStatusLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String platformType;

    private String orderId;

    private String channelOrderId;

    private Long storeId;

    private String beforeOrderStatus;

    private String afterOrderStatus;

    private String beforeDeliveryStatus;

    private String afterDeliveryStatus;

    private String beforePlatformStatus;

    private String afterPlatformStatus;

    private String changeSource;

    private String changeReason;

    private String snapshotContent;

    private Long createTime;
}
