package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 仓库采购订单新增/修改 Request VO")
@Data
public class WarehousePurchaseSaveReqVO {

    @Schema(description = "采购订单ID", example = "1")
    private Long purchaseOrderId;

    @Schema(description = "供应商ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "SUP001")
    @NotNull(message = "供应商不能为空")
    private String supplierId;

    @Schema(description = "收货仓库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "W001")
    @NotNull(message = "收货仓库不能为空")
    private String warehouseId;

    @Schema(description = "采购日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "采购日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate purchaseDate;

    @Schema(description = "采购员", example = "张三")
    private String purchaser;

    @Schema(description = "收货地址", example = "上海市浦东新区XX路1号")
    private String receiveAddress;

    @Schema(description = "备注", example = "加急采购")
    private String remark;

    @Schema(description = "采购单明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "采购单明细不能为空")
    @Valid
    private List<Item> items;

    @Schema(description = "管理后台 - 仓库采购订单明细新增/修改 Request VO")
    @Data
    public static class Item {

        @Schema(description = "明细ID", example = "1")
        private Long detailId;

        @Schema(description = "商品SKU ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @NotNull(message = "商品 SKU 不能为空")
        private Long productSkuId;

        @Schema(description = "采购数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
        @NotNull(message = "采购数量不能为空")
        private Integer purchaseQty;

        @Schema(description = "箱数", example = "1")
        private Integer boxQty;

        @Schema(description = "标准装箱数量", example = "10")
        private Integer standardBoxQty;

        @Schema(description = "采购单价", example = "12.50")
        private BigDecimal purchasePrice;
    }

}
