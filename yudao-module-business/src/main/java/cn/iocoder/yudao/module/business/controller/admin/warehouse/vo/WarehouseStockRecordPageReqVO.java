package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 仓库库存流水分页 Request VO")
@Data
public class WarehouseStockRecordPageReqVO extends PageParam {

    @Schema(description = "库存流水ID", example = "1")
    private Long stockRecordId;

    @Schema(description = "仓库ID", example = "W001")
    private String warehouseId;

    @Schema(description = "仓库商品ID", example = "1")
    private Long warehouseProductId;

    @Schema(description = "商品SKU ID", example = "1")
    private Long productSkuId;

    @Schema(description = "SKU编码")
    private String skuCode;

    @Schema(description = "SKU名称")
    private String skuName;

    @Schema(description = "业务类型", example = "70")
    private String bizType;

    @Schema(description = "业务单号", example = "CGDD20260416000001")
    private String bizNo;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
