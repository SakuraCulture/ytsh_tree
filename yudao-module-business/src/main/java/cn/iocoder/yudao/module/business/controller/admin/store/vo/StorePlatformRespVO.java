package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 门店平台关联 Response VO")
@Data
public class StorePlatformRespVO {

    @Schema(description = "平台关联ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long storePlatformId;

    @Schema(description = "门店ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String storeId;

    @Schema(description = "平台ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long platformId;

    @Schema(description = "平台名称")
    private String platformName;

    @Schema(description = "平台门店ID")
    private String platformStoreId;

    @Schema(description = "平台门店名称")
    private String platformStoreName;

    @Schema(description = "代理商类型")
    private String agentType;

    @Schema(description = "佣金比例")
    private BigDecimal commissionRate;

    @Schema(description = "结算账户")
    private String settlementAccount;

    @Schema(description = "状态")
    private Integer status;

}
