package cn.iocoder.yudao.module.ele.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单平台信息表实体类
 * 表名: order_platform_table
 */
@TableName("order_platform_table")
@Data
public class OrderPlatformDO {

    /** 订单ID(关联order_table.order_id) */
    private String orderId;

    /** 平台类型(ELE/MT/JD) */
    private String platformType;

    /** 配送平台 1-饿了么 2-美团 3-京东 4-其他 */
    private Integer deliveryPlatform;

    /** 配送类型 1-即时配送 2-预约配送 */
    private Integer deliveryType;

    /** 平台佣金(元) */
    private BigDecimal platformCommissionFee;

    /** 平台订单状态 */
    private String platformOrderStatus;

    /** 平台配送状态 */
    private String platformDeliveryStatus;

    /** 平台扩展信息(JSON) */
    private String platformExtend;

    /** ETL时间 */
    private java.time.LocalDateTime etlTime;

    /** 租户编号 */
    private Long tenantId;

    /** 创建人 */
    private String creator;

    /** 创建时间 */
    private Long createTime;

    /** 更新人 */
    private String updater;

    /** 更新时间 */
    private Long updateTime;

    /** 是否删除 0-否 1-是 */
    private Boolean deleted;
}
