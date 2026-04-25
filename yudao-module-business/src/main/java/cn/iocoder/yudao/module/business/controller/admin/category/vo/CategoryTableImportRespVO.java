package cn.iocoder.yudao.module.business.controller.admin.category.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 类目导入 Response VO")
@Data
@Builder
public class CategoryTableImportRespVO {

    @Schema(description = "创建成功的类目名称数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> createCategoryNames;

    @Schema(description = "更新成功的类目名称数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> updateCategoryNames;

    @Schema(description = "导入失败的类目集合，key 为类目名称，value 为失败原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> failureCategoryNames;

}
