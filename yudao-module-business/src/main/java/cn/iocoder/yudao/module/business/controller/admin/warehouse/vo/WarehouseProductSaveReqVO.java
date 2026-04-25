package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 仓库商品新增/修改 Request VO")
@Data
public class WarehouseProductSaveReqVO {

    @Schema(description = "仓库商品ID", example = "1")
    private Long warehouseProductId;

    @Schema(description = "仓库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "W001")
    private String warehouseId;

    @Schema(description = "SKU ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long productSkuId;

    @Schema(description = "仓库采购价", example = "10.50")
    private BigDecimal warehouseProductCostPrice;

    @Schema(description = "库位编码", example = "A-01-01")
    private String warehouseProductLocation;

    @Schema(description = "首次有库存日期", example = "2026-04-16")
    private LocalDate warehouseProductFirstDate;

    @Schema(description = "最近入库日期", example = "2026-04-16")
    private LocalDate warehouseProductLastDate;

}
