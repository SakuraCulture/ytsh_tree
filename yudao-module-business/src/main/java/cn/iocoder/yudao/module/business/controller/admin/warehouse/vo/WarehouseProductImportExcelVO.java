package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseProductImportExcelVO {

    @ExcelProperty("仓库名称")
    private String warehouseName;

    @ExcelProperty("SKU编码")
    private String productSkuCode;

    @ExcelProperty("采购价")
    private BigDecimal warehouseProductCostPrice;

    @ExcelProperty("库位")
    private String warehouseProductLocation;

    @ExcelProperty("首次入库日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private String warehouseProductFirstDate;

    @ExcelProperty("最近入库日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private String warehouseProductLastDate;
}
