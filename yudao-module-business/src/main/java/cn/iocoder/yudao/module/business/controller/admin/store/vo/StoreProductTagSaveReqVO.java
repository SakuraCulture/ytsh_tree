package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 门店商品手动标签保存 Request VO")
@Data
public class StoreProductTagSaveReqVO {

    @Schema(description = "门店商品 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "SP-001")
    @NotBlank(message = "门店商品 ID 不能为空")
    private String storeProductId;

    @Schema(description = "标签值 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "标签值 ID 列表不能为空")
    private List<Long> tagValueIds;

}
