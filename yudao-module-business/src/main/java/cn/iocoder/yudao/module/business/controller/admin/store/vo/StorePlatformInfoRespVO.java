package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 门店平台简单信息 Response VO")
@Data
public class StorePlatformInfoRespVO {

    @Schema(description = "平台门店ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String platformStoreId;

    @Schema(description = "平台门店名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String storeName;

}