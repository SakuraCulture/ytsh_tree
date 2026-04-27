package cn.iocoder.yudao.module.business.controller.admin.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - SKU商品主数据聚合 Response VO")
@Data
public class SkuTableAggregateRespVO extends SkuTableRespVO {

    @Schema(description = "SKU条码列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<UpcTableRespVO> upcTables;

}
