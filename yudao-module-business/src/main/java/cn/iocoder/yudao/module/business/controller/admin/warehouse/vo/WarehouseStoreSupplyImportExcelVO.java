package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseStoreSupplyImportExcelVO {

    @ExcelProperty("仓库ID")
    private String warehouseId;

    @ExcelProperty("仓库名称")
    private String warehouseName;

    @ExcelProperty("门店ID")
    private String storeId;

    @ExcelProperty("门店名称")
    private String storeName;

    @ExcelProperty("是否主仓")
    private Integer isPrimary;

    @ExcelProperty("状态")
    private Integer supplyStatus;

    @ExcelProperty("备注")
    private String remark;
}
