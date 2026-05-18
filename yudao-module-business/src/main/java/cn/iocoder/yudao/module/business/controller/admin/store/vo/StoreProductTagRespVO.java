package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import cn.iocoder.yudao.module.business.controller.admin.product.vo.ProductSpuTagSourceRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 门店商品标签 Response VO")
@Data
public class StoreProductTagRespVO {

    @Schema(description = "标签值 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long tagValueId;

    @Schema(description = "标签值编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "store_focus")
    private String tagValueCode;

    @Schema(description = "标签值名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "门店重点陈列")
    private String tagValueName;

    @Schema(description = "维度路径", requiredMode = Schema.RequiredMode.REQUIRED, example = "L1 / L2 / L3")
    private String dimensionPath;

    @Schema(description = "来源类型列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> sources;

    @Schema(description = "来源明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ProductSpuTagSourceRespVO> sourceDetails;

}
