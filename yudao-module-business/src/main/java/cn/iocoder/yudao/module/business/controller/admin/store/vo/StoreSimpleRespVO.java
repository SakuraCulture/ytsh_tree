package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "管理后台 - 门店简单信息 Response VO")
@Data
public class StoreSimpleRespVO {

    @Schema(description = "门店ID", example = "S001")
    private String storeId;

    @Schema(description = "门店名称", example = "张三门店")
    private String storeName;

    @Schema(description = "平台ID", example = "1")
    private Long platformId;

    @Schema(description = "平台方门店ID", example = "P001")
    private String platformStoreId;

    @Schema(description = "门店状态(0停用1正常)", example = "1")
    private Integer storeStatus;

}
