package cn.iocoder.yudao.module.ele.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 订单跟踪实体类
 * 表名: ele_order_tracking
 */
@TableName("ele_order_tracking")
@KeySequence("ele_order_tracking_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EleOrderTrackingDO extends BaseDO {

    /** 主键ID */
    @TableId
    private Long id;

    /** 订单号 */
    private String orderId;

    /** 平台门店ID */
    private String platformStoreId;

    /** SAAS商家编码 */
    private String merchantCode;

    /** 外部门店编码 */
    private String erpStoreCode;

    /** 外部渠道订单ID */
    private String channelOrderId;

    /** 当前订单状态 */
    private Integer orderStatus;

    /** 订单创建时间（秒级时间戳） */
    private Long orderCreateTime;

    /** 最后一次推送时间（秒级时间戳） */
    private Long lastPushTime;

    /** 最后一次推送的订单状态 */
    private Integer lastPushStatus;

    /** 跟踪状态: TRACKING-跟踪中, COMPLETED-已完结, TIMEOUT-超时告警 */
    private String trackingStatus;

    /** 告警级别: NULL-无告警, WARNING-3天警告, CRITICAL-5天严重 */
    private String alertLevel;

    /** 是否已显示告警: 0-未显示, 1-已显示 */
    private Integer alertShown;

    /** 备注 */
    private String remark;
}
