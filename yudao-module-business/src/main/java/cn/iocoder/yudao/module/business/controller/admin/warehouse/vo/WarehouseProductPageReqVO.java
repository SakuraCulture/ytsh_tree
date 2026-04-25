package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 仓库商品分页 Request VO")
@Data
public class WarehouseProductPageReqVO extends PageParam {

    @Schema(description = "仓库商品ID", example = "1")
    private Long warehouseProductId;

    @Schema(description = "仓库ID", example = "W001")
    private String warehouseId;

    @Schema(description = "SKU ID", example = "1")
    private Long productSkuId;

    @Schema(description = "SKU编码")
    private String skuCode;

    @Schema(description = "SKU名称")
    private String skuName;

    @Schema(description = "库位编码", example = "A-01-01")
    private String warehouseProductLocation;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
