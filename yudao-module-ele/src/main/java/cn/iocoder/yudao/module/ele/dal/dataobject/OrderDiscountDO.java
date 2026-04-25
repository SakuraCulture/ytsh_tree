package cn.iocoder.yudao.module.ele.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单优惠信息表实体类
 * 表名: order_discount_table
 */
@TableName("order_discount_table")
@Data
public class OrderDiscountDO {

    /** 优惠ID */
    @TableId(type = IdType.AUTO)
    private Long discountId;

    /** 订单ID(关联order_table.order_id) */
    private String orderId;

    /** 关联子订单ID */
    private String subOrderId;

    /** 活动ID */
    private String activityId;

    /** 活动名称 */
    private String activityName;

    /** 活动归属订单类型 1-主单 0-子单 */
    private String activityOrderType;

    /** 优惠类型(FULL_REDUCTION-满减, COUPON-优惠券, NEW_USER-新用户, etc) */
    private String discountType;

    /** 优惠金额(元) */
    private BigDecimal discountFee;

    /** 商户承担金额(元) */
    private BigDecimal merchantFee;

    /** 平台承担金额(元) */
    private BigDecimal platformFee;

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