package cn.iocoder.yudao.module.business.controller.admin.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - SPU基础分类聚合 Response VO")
@Data
public class SpuTableAggregateRespVO {

    @Schema(description = "SPU ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "20081")
    private Long productSpuId;

    @Schema(description = "SPU编码")
    private String productSpuCode;

    @Schema(description = "SPU名称", example = "芋艿")
    private String productSpuName;

    @Schema(description = "品牌")
    private String productBrand;

    @Schema(description = "分类ID", example = "27572")
    private Long categoryId;

    @Schema(description = "产地")
    private String productOrigin;

    @Schema(description = "生产商")
    private String productManufacturer;

    @Schema(description = "规格模板")
    private String productSpecTemplate;

    @Schema(description = "商品主图URL", example = "https://www.iocoder.cn")
    private String productImageUrl;

    @Schema(description = "商品详情图片")
    private String productDetailImages;

    @Schema(description = "商品描述", example = "随便")
    private String productDescription;

    @Schema(description = "状态(0下架1上架)", example = "1")
    private Integer productSpuStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "标签列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ProductSpuTagRespVO> tags;

    @Schema(description = "SKU商品主数据列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<SkuTableAggregateRespVO> skuTables;

}
