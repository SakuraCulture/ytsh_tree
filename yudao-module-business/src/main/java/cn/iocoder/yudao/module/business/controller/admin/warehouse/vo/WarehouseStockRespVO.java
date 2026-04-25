package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 仓库库存 Response VO")
@Data
@ExcelIgnoreUnannotated
public class WarehouseStockRespVO {

    @Schema(description = "仓库库存ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("仓库库存ID")
    private Long warehouseStockId;

    @Schema(description = "仓库商品ID", example = "1")
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

    @Schema(description = "库存数量", example = "100")
    @ExcelProperty("库存数量")
    private Integer warehouseStockQty;

    @Schema(description = "可用量", example = "100")
    @ExcelProperty("可用量")
    private Integer warehouseStockAvailableQty;

    @Schema(description = "在途数量", example = "0")
    @ExcelProperty("在途数量")
    private Integer warehouseStockTransitQty;

    @Schema(description = "冻结库存", example = "0")
    @ExcelProperty("冻结库存")
    private Integer warehouseStockFrozenQty;

    @Schema(description = "缺货时长(小时)", example = "0")
    @ExcelProperty("缺货时长(小时)")
    private Integer warehouseStockOutstockHours;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
