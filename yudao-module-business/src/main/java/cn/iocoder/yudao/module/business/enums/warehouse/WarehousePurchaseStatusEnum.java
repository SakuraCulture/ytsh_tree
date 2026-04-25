package cn.iocoder.yudao.module.business.enums.warehouse;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 仓库采购单状态枚举
 */
@RequiredArgsConstructor
@Getter
public enum WarehousePurchaseStatusEnum {

    DRAFT("0", "草稿"),
    PENDING_AUDIT("1", "待审核"),
    APPROVED("2", "已审核"),
    INBOUND("3", "已入库"),
    COMPLETED("4", "已完成"),
    CANCELLED("5", "已取消");

    private final String status;
    private final String name;

}
