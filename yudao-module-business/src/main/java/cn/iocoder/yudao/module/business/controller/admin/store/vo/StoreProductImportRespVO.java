package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 门店商品导入 Response VO")
@Data
@Builder
public class StoreProductImportRespVO {

    @Schema(description = "创建成功的门店商品ID数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> createStoreProductIds;

    @Schema(description = "更新成功的门店商品ID数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> updateStoreProductIds;

    @Schema(description = "导入失败的门店商品集合，key 为门店商品ID，value 为失败原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> failureStoreProductIds;

}