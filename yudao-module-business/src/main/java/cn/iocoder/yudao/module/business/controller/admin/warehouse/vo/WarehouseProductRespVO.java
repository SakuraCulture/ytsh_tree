package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 仓库商品 Response VO")
@Data
@ExcelIgnoreUnannotated
public class WarehouseProductRespVO {

    @Schema(description = "仓库商品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("仓库商品ID")
    private Long warehouseProductId;

    @Schema(description = "仓库ID", example = "W001")
    @ExcelProperty("仓库ID")
    private String warehouseId;

    @Schema(description = "仓库名称", example = "华东一仓")
    @ExcelProperty("仓库名称")
    private String warehouseName;

    @Schema(description = "SKU ID", example = "1")
    @ExcelProperty("SKU ID")
    private Long productSkuId;

    @Schema(description = "SKU编码", example = "SKU001")
    @ExcelProperty("SKU编码")
    private String skuCode;

    @Schema(description = "SKU名称", example = "白色款式")
    @ExcelProperty("SKU名称")
    private String skuName;

    @Schema(description = "基准零售价")
    @ExcelProperty("基准零售价")
    private BigDecimal retailPrice;

    @Schema(description = "仓库采购价")
    @ExcelProperty("仓库采购价")
    private BigDecimal warehouseProductCostPrice;

    @Schema(description = "库位编码")
    @ExcelProperty("库位编码")
    private String warehouseProductLocation;

    @Schema(description = "首次有库存日期")
    @ExcelProperty("首次有库存日期")
    private LocalDate warehouseProductFirstDate;

    @Schema(description = "最近入库日期")
    @ExcelProperty("最近入库日期")
    private LocalDate warehouseProductLastDate;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
