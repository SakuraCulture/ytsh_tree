package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 门店导入 Response VO")
@Data
@Builder
public class StoreImportRespVO {

    @Schema(description = "创建成功的门店名称数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> createStoreNames;

    @Schema(description = "更新成功的门店名称数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> updateStoreNames;

    @Schema(description = "导入失败的门店集合，key 为门店名称，value 为失败原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> failureStoreNames;

}
