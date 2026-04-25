package cn.iocoder.yudao.module.business.enums.warehouse;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 仓库库存流水业务类型枚举
 */
@RequiredArgsConstructor
@Getter
public enum WarehouseStockRecordBizTypeEnum {

    PURCHASE_IN("70", "采购入库");

    private final String type;
    private final String name;

}
