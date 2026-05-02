package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 平台门店简表批量查询 Request VO")
@Data
public class StorePlatformSimpleBatchReqVO {

    @Schema(description = "平台ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "平台ID不能为空")
    private Long platformId;

    @Schema(description = "平台门店ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "平台门店ID列表不能为空")
    private List<String> platformStoreIds;
}
