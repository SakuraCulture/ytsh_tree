package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 仓库商品简单 Response VO")
@Data
public class WarehouseProductSimpleRespVO {

    @Schema(description = "仓库商品ID", example = "1")
    private Long warehouseProductId;

    @Schema(description = "仓库ID", example = "W001")
    private String warehouseId;

    @Schema(description = "SKU ID", example = "1")
    private Long productSkuId;

    @Schema(description = "SKU名称", example = "白色款式")
    private String skuName;

}
