package cn.iocoder.yudao.module.business.controller.admin.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 商品 SPU 简要标签 Response VO")
@Data
public class ProductSpuTagSimpleRespVO {

    @Schema(description = "商品 SPU ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long productSpuId;

    @Schema(description = "标签列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ProductSpuTagRespVO> tags;

}
