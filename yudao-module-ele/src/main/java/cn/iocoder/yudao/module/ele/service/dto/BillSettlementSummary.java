package cn.iocoder.yudao.module.ele.service.dto;

import lombok.Data;

@Data
public class BillSettlementSummary {
    private String settleOrderId;
    private String orderId;
    private Long totalAmount;
    private Long positiveAmount;
    private Long operationAmount;
    private Integer billCount;
    private Integer settleOrderCount;
}
