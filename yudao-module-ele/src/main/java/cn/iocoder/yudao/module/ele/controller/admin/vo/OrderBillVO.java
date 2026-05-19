package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 订单账单 Response VO")
@Data
public class OrderBillVO {

    @Schema(description = "订单号")
    private String orderId;

    @Schema(description = "账单明细列表(一个订单可能有多条账单)")
    private List<BillDetailVO> billDetails;

    @Schema(description = "汇总-结算金额(元)")
    private BigDecimal totalBillAmount;

    @Schema(description = "汇总-结算状态(0未结算/1已结算)")
    private Integer totalStatus;

    @Schema(description = "账单明细 VO")
    @Data
    public static class BillDetailVO {
        @Schema(description = "账单唯一ID")
        private String billId;

        @Schema(description = "账单日期")
        private String billDate;

        @Schema(description = "订单日期")
        private String orderDate;

        @Schema(description = "结算状态(0未结算/1已结算)")
        private Integer status;

        @Schema(description = "结算状态文本")
        private String statusText;

        @Schema(description = "结算金额(元)")
        private BigDecimal billAmount;

        @Schema(description = "商品原价(元)")
        private BigDecimal itemPrice;

        @Schema(description = "包装费原价(元)")
        private BigDecimal packageFee;

        @Schema(description = "配送费原价(元)")
        private BigDecimal deliveryFee;

        @Schema(description = "商家活动费用(元)")
        private BigDecimal shopMarketingFee;

        @Schema(description = "平台费用(元)")
        private BigDecimal platformFee;

        @Schema(description = "公益捐赠(元)")
        private BigDecimal donationFee;

        @Schema(description = "用户支付配送费(元)")
        private BigDecimal userPayShippingAmount;

        @Schema(description = "用户实付金额(元)")
        private BigDecimal userOnlinePayAmount;

        @Schema(description = "商家商品补贴(元)")
        private BigDecimal productPreferences;

        @Schema(description = "商家配送费补贴(元)")
        private BigDecimal notProductPreferences;

        @Schema(description = "商家平台配送服务费(元)")
        private BigDecimal performanceServiceFee;

        @Schema(description = "商家平台服务费(元)")
        private BigDecimal platformChargeFee;

        @Schema(description = "平台活动补贴(元)")
        private BigDecimal activityAmount;

        @Schema(description = "结算类型")
        private String billTypeDesc;

        @Schema(description = "配送方式")
        private String shippingType;

        @Schema(description = "结算ID")
        private String settleOrderId;

        @Schema(description = "退款ID")
        private String refundId;

        @Schema(description = "渠道店ID")
        private String shopId;

        @Schema(description = "创建时间")
        private String createTime;

        @Schema(description = "更新时间")
        private String updateTime;
    }
}
