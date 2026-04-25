package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Schema(description = "管理后台 - SKU简单列表 Response VO")
@Data
public class SkuSimpleRespVO {

    @Schema(description = "SKU ID", example = "1")
    private Long productSkuId;

    @Schema(description = "SKU编码", example = "SKU001")
    private String productSkuCode;

    @Schema(description = "SKU名称", example = "白色款式")
    private String productSkuName;

    @Schema(description = "主EAN码", example = "1234567890123")
    private String productSkuEan;

    @Schema(description = "基准零售价")
    private BigDecimal productRetailPrice;

    @Schema(description = "状态(0下架1上架)", example = "1")
    private Integer productSkuStatus;

}