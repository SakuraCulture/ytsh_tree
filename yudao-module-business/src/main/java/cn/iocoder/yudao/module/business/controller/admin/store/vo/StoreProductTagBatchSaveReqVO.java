package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 门店商品批量标签保存 Request VO")
@Data
public class StoreProductTagBatchSaveReqVO {

    @Schema(description = "门店商品 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "门店商品 ID 列表不能为空")
    private List<String> storeProductIds;

    @Schema(description = "标签值 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "标签值 ID 列表不能为空")
    private List<Long> tagValueIds;

}
