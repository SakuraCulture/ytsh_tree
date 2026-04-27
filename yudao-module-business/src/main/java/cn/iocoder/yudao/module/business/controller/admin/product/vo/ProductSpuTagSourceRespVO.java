package cn.iocoder.yudao.module.business.controller.admin.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 商品 SPU 标签来源 Response VO")
@Data
public class ProductSpuTagSourceRespVO {

    @Schema(description = "来源类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "MANUAL")
    private String sourceType;

    @Schema(description = "来源引用", requiredMode = Schema.RequiredMode.REQUIRED, example = "rule-price")
    private String sourceRef;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "生效时间")
    private LocalDateTime effectiveTime;

    @Schema(description = "失效时间")
    private LocalDateTime expireTime;

}
