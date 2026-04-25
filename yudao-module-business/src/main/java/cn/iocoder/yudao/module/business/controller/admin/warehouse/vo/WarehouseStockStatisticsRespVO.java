package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - 仓库库存统计 Response VO")
@Data
@Builder
public class WarehouseStockStatisticsRespVO {

    @Schema(description = "库存记录数", example = "10")
    private Long stockCount;

    @Schema(description = "总库存量", example = "100")
    private Integer totalQuantity;

    @Schema(description = "总可用量", example = "80")
    private Integer totalAvailableQuantity;

    @Schema(description = "总在途量", example = "10")
    private Integer totalTransitQuantity;

    @Schema(description = "总冻结量", example = "10")
    private Integer totalFrozenQuantity;

}
