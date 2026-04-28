package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 饿了么门店商品影子 Response VO")
@Data
public class EleStoreGoodsShadowRespVO {

    @Schema(description = "影子记录 ID")
    private Long id;

    @Schema(description = "平台 ID")
    private Long platformId;

    @Schema(description = "商家编码")
    private String merchantCode;

    @Schema(description = "ERP 门店编码")
    private String erpStoreCode;

    @Schema(description = "平台门店编码")
    private String platformStoreId;

    @Schema(description = "门店 ID")
    private String storeId;

    @Schema(description = "SPU 编码")
    private String spuCode;

    @Schema(description = "SKU 编码")
    private String skuCode;

    @Schema(description = "子 SKU 编码")
    private String subSkuCode;

    @Schema(description = "商品名称")
    private String title;

    @Schema(description = "主图")
    private String mainPic;

    @Schema(description = "规格")
    private String specification;

    @Schema(description = "门店售价")
    private BigDecimal salePrice;

    @Schema(description = "上下架状态")
    private String posStatus;

    @Schema(description = "是否启用")
    private Integer isActive;

    @Schema(description = "匹配状态")
    private String matchStatus;

    @Schema(description = "匹配到的本地 SKU ID")
    private String matchedProductSkuId;

    @Schema(description = "归并后的正式门店商品 ID")
    private String mergedStoreProductId;

    @Schema(description = "冲突原因")
    private String conflictReason;

    @Schema(description = "原始报文")
    private String rawPayload;

    @Schema(description = "最近同步时间")
    private LocalDateTime lastSyncTime;

    @Schema(description = "匹配时间")
    private LocalDateTime matchedTime;

    @Schema(description = "归并时间")
    private LocalDateTime mergedTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
