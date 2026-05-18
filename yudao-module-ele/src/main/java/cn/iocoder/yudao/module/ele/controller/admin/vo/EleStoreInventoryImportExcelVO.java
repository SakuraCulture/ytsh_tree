package cn.iocoder.yudao.module.ele.controller.admin.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class EleStoreInventoryImportExcelVO {

    @ExcelProperty("ERP门店编码")
    private String erpStoreCode;

    @ExcelProperty("SKU编码")
    private String skuCode;

    @ExcelProperty("子SKU编码")
    private String subSkuCode;

    @ExcelProperty("总库存")
    private Integer physicalStockTotalAmount;

    @ExcelProperty("可售库存")
    private Integer availableForSale;

    @ExcelProperty("预留库存")
    private Integer reservedAmount;

    @ExcelProperty("物理可用库存")
    private Integer physicalStockAvailableAmount;

    @ExcelProperty("物理占用库存")
    private Integer physicalStockOccupiedAmount;

    @ExcelProperty("物理在途库存")
    private Integer physicalStockIntransitAmount;

    @ExcelProperty("备注")
    private String remark;
}
