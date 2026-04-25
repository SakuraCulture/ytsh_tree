package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StoreProductImportExcelVO {

    @ExcelProperty("门店ID")
    private String storeId;

    @ExcelProperty("SKU ID")
    private String productSkuId;

    @ExcelProperty("归属")
    private String storeProductOwnership;

    @ExcelProperty("POS状态")
    private String storeProductPosStatus;

    @ExcelProperty("价格")
    private BigDecimal storeProductPrice;

    @ExcelProperty("是否启用(0否1是)")
    private Integer storeProductIsActive;

}