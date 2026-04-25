package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 饿了么门店商品待治理池 Response VO")
@Data
public class EleStoreGoodsGovernancePoolRespVO {

    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "商家编码")
    private String merchantCode;

    @Schema(description = "ERP 门店编码")
    private String erpStoreCode;

    @Schema(description = "平台 ID")
    private Long platformId;

    @Schema(description = "门店 ID")
    private String storeId;

    @Schema(description = "平台门店编码")
    private String platformStoreId;

    @Schema(description = "SKU 编码")
    private String skuCode;

    @Schema(description = "子 SKU 编码")
    private String subSkuCode;

    @Schema(description = "SPU 编码")
    private String spuCode;

    @Schema(description = "商品层级")
    private String goodsLevel;

    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "治理原因编码")
    private String reasonCode;

    @Schema(description = "治理原因说明")
    private String reasonMsg;

    @Schema(description = "处理状态")
    private String processStatus;

    @Schema(description = "原始报文")
    private String rawPayload;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
