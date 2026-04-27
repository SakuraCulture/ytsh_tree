package cn.iocoder.yudao.module.business.controller.admin.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 商品 SPU 手动标签保存 Request VO")
@Data
public class ProductSpuTagSaveReqVO {

    @Schema(description = "商品 SPU ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "商品 SPU ID 不能为空")
    private Long productSpuId;

    @Schema(description = "标签值 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "标签值 ID 列表不能为空")
    private List<Long> tagValueIds;

}
