package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 门店商品分页 Request VO")
@Data
public class StoreProductPageReqVO extends PageParam {

    @Schema(description = "门店商品ID", example = "SP001")
    private String storeProductId;

    @Schema(description = "门店ID", example = "S001")
    private String storeId;

    @Schema(description = "SKU ID", example = "SKU001")
    private String productSkuId;

    @Schema(description = "SKU编码")
    private String skuCode;

    @Schema(description = "SKU名称")
    private String skuName;

    @Schema(description = "商品归属")
    private String productAttribution;

    @Schema(description = "POS状态")
    private Integer posStatus;

    @Schema(description = "行来源")
    private String rowSource;

    @Schema(description = "完整性状态")
    private String completenessStatus;

    @Schema(description = "匹配状态")
    private String matchStatus;

    @Schema(description = "入店状态(0否1是)", example = "1")
    private Integer enterShopStatus;

    @Schema(description = "标签值 ID", example = "1001")
    private Long tagValueId;

    @Schema(description = "门店商品 ID 列表（内部使用）")
    private List<String> storeProductIds;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
