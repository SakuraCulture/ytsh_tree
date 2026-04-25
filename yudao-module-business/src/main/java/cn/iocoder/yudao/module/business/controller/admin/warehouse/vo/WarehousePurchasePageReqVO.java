package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 仓库采购订单分页 Request VO")
@Data
public class WarehousePurchasePageReqVO extends PageParam {

    @Schema(description = "采购订单ID", example = "1")
    private Long purchaseOrderId;

    @Schema(description = "采购单号", example = "CGDD20260416000001")
    private String purchaseOrderNo;

    @Schema(description = "供应商ID", example = "SUP001")
    private String supplierId;

    @Schema(description = "收货仓库ID", example = "W001")
    private String warehouseId;

    @Schema(description = "采购单状态", example = "1")
    private String orderStatus;

    @Schema(description = "收货状态", example = "1")
    private String receiveStatus;

    @Schema(description = "商品SKU ID", example = "1")
    private Long productSkuId;

    @Schema(description = "SKU编码")
    private String skuCode;

    @Schema(description = "SKU名称")
    private String skuName;

    @Schema(description = "采购员")
    private String purchaser;

    @Schema(description = "采购日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] purchaseDate;

    @Schema(description = "审核日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] auditDate;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
