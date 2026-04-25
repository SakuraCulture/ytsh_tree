package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 仓库库存流水 Response VO")
@Data
@ExcelIgnoreUnannotated
public class WarehouseStockRecordRespVO {

    @Schema(description = "库存流水ID", example = "1")
    @ExcelProperty("库存流水ID")
    private Long stockRecordId;

    @Schema(description = "仓库ID", example = "W001")
    @ExcelProperty("仓库ID")
    private String warehouseId;

    @Schema(description = "仓库名称", example = "华东一仓")
    @ExcelProperty("仓库名称")
    private String warehouseName;

    @Schema(description = "仓库商品ID", example = "1")
    @ExcelProperty("仓库商品ID")
    private Long warehouseProductId;

    @Schema(description = "商品SKU ID", example = "1")
    @ExcelProperty("商品SKU ID")
    private Long productSkuId;

    @Schema(description = "SKU编码", example = "SKU001")
    @ExcelProperty("SKU编码")
    private String skuCode;

    @Schema(description = "SKU名称", example = "白色款式")
    @ExcelProperty("SKU名称")
    private String skuName;

    @Schema(description = "业务类型", example = "70")
    @ExcelProperty("业务类型")
    private String bizType;

    @Schema(description = "业务单号", example = "CGDD20260416000001")
    @ExcelProperty("业务单号")
    private String bizNo;

    @Schema(description = "业务ID", example = "1")
    @ExcelProperty("业务ID")
    private Long bizId;

    @Schema(description = "业务项ID", example = "1")
    @ExcelProperty("业务项ID")
    private Long bizItemId;

    @Schema(description = "变动数量", example = "10")
    @ExcelProperty("变动数量")
    private Integer changeQty;

    @Schema(description = "变动后库存", example = "100")
    @ExcelProperty("变动后库存")
    private Integer afterQty;

    @Schema(description = "创建人", example = "admin")
    @ExcelProperty("创建人")
    private String creator;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
