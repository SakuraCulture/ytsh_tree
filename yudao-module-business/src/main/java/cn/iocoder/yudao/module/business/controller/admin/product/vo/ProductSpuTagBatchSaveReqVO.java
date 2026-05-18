package cn.iocoder.yudao.module.business.controller.admin.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 商品 SPU 批量标签保存 Request VO")
@Data
public class ProductSpuTagBatchSaveReqVO {

    @Schema(description = "商品 SPU ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "商品 SPU ID 列表不能为空")
    private List<Long> productSpuIds;

    @Schema(description = "标签值 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "标签值 ID 列表不能为空")
    private List<Long> tagValueIds;

}
