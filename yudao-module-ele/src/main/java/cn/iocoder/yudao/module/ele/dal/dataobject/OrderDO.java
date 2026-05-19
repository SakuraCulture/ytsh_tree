package cn.iocoder.yudao.module.ele.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单主表实体类
 * 表名: order_table
 */
@TableName("order_table")
@Data
public class OrderDO {

    /** 订单ID */
    @TableId
    private String orderId;

    /** 用户ID */
    private String userId;

    /** 订单状态 1-待支付 2-已支付 3-配送中 4-已完成 5-已取消 */
    private Integer orderStatus;

    /** 创建时间 */
    private Long createTime;

    /** 支付时间 */
    private Long payTime;

    /** 买家名称 */
    private String buyerName;

    /** 买家电话 */
    private String buyerPhone;

    /** 买家地址 */
    private String buyerAddress;

    /** 配送员名称 */
    private String deliveryName;

    /** 配送员电话 */
    private String deliveryPhone;

    /** 配送状态 1-待配送 2-配送中 3-已配送 */
    private Integer deliveryStatus;

    /** 订单总金额(元) */
    private BigDecimal totalFee;

    /** 实付金额(元) */
    private BigDecimal payFee;

    /** 优惠金额(元) */
    private BigDecimal discountFee;

    /** 配送费(元) */
    private BigDecimal deliveryFee;

    /** 邮费(元) */
    private BigDecimal postFee;

    /** 包装费(元) */
    private BigDecimal packageFee;

    /** 渠道来源ID */
    private String channelSourceId;

    /** 渠道来源名称(饿了么/美团等) */
    private String channelSourceName;

    /** 渠道订单号 */
    private String channelOrderId;

    /** 门店编码 */
    private String storeCode;

    /** 经度 */
    private String longitude;

    /** 纬度 */
    private String latitude;

    /** 订单备注 */
    private String remark;

    /** 预约类型 1-预约单 2-即时单 */
    private Integer arriveType;

    /** 关联门店ID */
    private Long storeId;

    /** 订单来源 */
    private String orderFrom;

    /** 当日流水号 */
    private Integer orderIndex;

    /** 预计收入(元) */
    private BigDecimal estimatedIncome;

    /** 子订单JSON数据 */
    private String subOrdersJson;

    /** 折扣信息JSON数据 */
    private String discountsJson;

    /** ETL时间 */
    private java.time.LocalDateTime etlTime;

    /** 租户编号 */
    private String tenantId;

    /** 创建人 */
    private String creator;

    /** 更新人 */
    private String updater;

    /** 更新时间 */
    private Long updateTime;

    /** 是否删除 0-否 1-是 */
    private Boolean deleted;

    /** 结算金额(元) */
    private java.math.BigDecimal settlementAmount;

    /** 结算状态(0未结算/1已结算) */
    private Integer settlementStatus;
}
