package cn.iocoder.yudao.module.business.controller.admin.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 商品 SPU 标签 Response VO")
@Data
public class ProductSpuTagRespVO {

    @Schema(description = "标签值 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long tagValueId;

    @Schema(description = "标签值编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "high_value_product")
    private String tagValueCode;

    @Schema(description = "标签值名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "高价值商品")
    private String tagValueName;

    @Schema(description = "维度路径", requiredMode = Schema.RequiredMode.REQUIRED, example = "L1 / L2 / L3")
    private String dimensionPath;

    @Schema(description = "来源类型列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> sources;

    @Schema(description = "来源明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ProductSpuTagSourceRespVO> sourceDetails;

}
