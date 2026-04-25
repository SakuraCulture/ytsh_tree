package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 门店商品新增/修改 Request VO")
@Data
public class StoreProductSaveReqVO {

    @Schema(description = "门店商品ID", example = "SP001")
    private String storeProductId;

    @Schema(description = "门店ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "S001")
    private String storeId;

    @Schema(description = "SKU ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "SKU001")
    private String productSkuId;

    @Schema(description = "归属", requiredMode = Schema.RequiredMode.REQUIRED)
    private String storeProductOwnership;

    @Schema(description = "POS状态")
    private String storeProductPosStatus;

    @Schema(description = "价格", example = "99.99")
    private BigDecimal storeProductPrice;

    @Schema(description = "是否启用(0否1是)", example = "1")
    private Integer storeProductIsActive;

    @Schema(description = "首次入店日期", example = "2026-04-15")
    private LocalDate storeProductFirstDate;

}
