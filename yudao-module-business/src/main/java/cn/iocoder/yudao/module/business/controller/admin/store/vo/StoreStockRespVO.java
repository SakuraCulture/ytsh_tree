package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "管理后台 - 门店库存 Response VO")
@Data
public class StoreStockRespVO {

    @Schema(description = "可用量")
    private Integer availableQuantity;

    @Schema(description = "库存数量")
    private Integer inventoryQuantity;

    @Schema(description = "在途数量")
    private Integer inTransitQuantity;

    @Schema(description = "冻结库存")
    private Integer frozenQuantity;

    @Schema(description = "缺货时长")
    private Integer outOfStockDuration;

}
