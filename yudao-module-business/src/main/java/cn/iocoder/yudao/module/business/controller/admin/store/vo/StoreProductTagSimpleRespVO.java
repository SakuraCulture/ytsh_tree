package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 门店商品简要标签 Response VO")
@Data
public class StoreProductTagSimpleRespVO {

    @Schema(description = "门店商品 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "SP-001")
    private String storeProductId;

    @Schema(description = "标签列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<StoreProductTagRespVO> tags;

}
