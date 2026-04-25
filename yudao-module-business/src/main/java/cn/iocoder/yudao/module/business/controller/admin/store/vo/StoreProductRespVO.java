package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 门店商品 Response VO")
@Data
@ExcelIgnoreUnannotated
public class StoreProductRespVO {

    @Schema(description = "门店商品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "SP001")
    @ExcelProperty("门店商品ID")
    private String storeProductId;

    @Schema(description = "门店ID", example = "S001")
    @ExcelProperty("门店ID")
    private String storeId;

    @Schema(description = "门店名称", example = "示例门店")
    @ExcelProperty("门店名称")
    private String storeName;

    @Schema(description = "SKU ID", example = "SKU001")
    @ExcelProperty("SKU ID")
    private String productSkuId;

    @Schema(description = "SKU编码", example = "SKU001")
    @ExcelProperty("SKU编码")
    private String skuCode;

    @Schema(description = "SKU名称", example = "白色款式")
    @ExcelProperty("SKU名称")
    private String skuName;

    @Schema(description = "商品归属")
    @ExcelProperty("商品归属")
    private String productAttribution;

    @Schema(description = "POS状态")
    @ExcelProperty("POS状态")
    private Integer posStatus;

    @Schema(description = "门店零售价")
    @ExcelProperty("门店零售价")
    private BigDecimal storeRetailPrice;

    @Schema(description = "入店状态(0否1是)", example = "1")
    @ExcelProperty("入店状态(0否1是)")
    private Integer enterShopStatus;

    @Schema(description = "首次入店日期")
    @ExcelProperty("首次入店日期")
    private LocalDate firstEnterShopDate;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
