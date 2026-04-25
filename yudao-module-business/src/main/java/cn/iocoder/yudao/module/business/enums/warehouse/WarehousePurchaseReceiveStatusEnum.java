package cn.iocoder.yudao.module.business.enums.warehouse;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 仓库采购收货状态枚举
 */
@RequiredArgsConstructor
@Getter
public enum WarehousePurchaseReceiveStatusEnum {

    PENDING_RECEIVE("1", "待收货"),
    PARTIAL_RECEIVE("2", "部分收货"),
    RECEIVED("3", "已收货"),
    DIFF("4", "有差异");

    private final String status;
    private final String name;

}
