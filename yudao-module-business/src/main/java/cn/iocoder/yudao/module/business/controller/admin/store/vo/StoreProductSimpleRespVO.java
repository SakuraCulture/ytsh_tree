package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "管理后台 - 门店商品简单列表 Response VO")
@Data
public class StoreProductSimpleRespVO {

    @Schema(description = "门店商品ID", example = "SP001")
    private String storeProductId;

    @Schema(description = "门店ID", example = "S001")
    private String storeId;

    @Schema(description = "SKU ID", example = "SKU001")
    private String productSkuId;

    @Schema(description = "归属")
    private String storeProductOwnership;

    @Schema(description = "是否启用(0否1是)", example = "1")
    private Integer storeProductIsActive;

}