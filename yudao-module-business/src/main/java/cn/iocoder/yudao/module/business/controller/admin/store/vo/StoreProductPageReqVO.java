package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

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

    @Schema(description = "入店状态(0否1是)", example = "1")
    private Integer enterShopStatus;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
