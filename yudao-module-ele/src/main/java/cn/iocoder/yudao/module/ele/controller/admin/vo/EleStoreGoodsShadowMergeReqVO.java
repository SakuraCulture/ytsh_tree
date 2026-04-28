package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 饿了么门店商品影子手动归并 Request VO")
@Data
public class EleStoreGoodsShadowMergeReqVO {

    @Schema(description = "匹配的本地 SKU ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "匹配SKU ID不能为空")
    private String matchedProductSkuId;
}
