package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 仓库采购订单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class WarehousePurchaseRespVO {

    @Schema(description = "采购订单ID", example = "1")
    @ExcelProperty("采购订单ID")
    private Long purchaseOrderId;

    @Schema(description = "采购单号", example = "CGDD20260416000001")
    @ExcelProperty("采购单号")
    private String purchaseOrderNo;

    @Schema(description = "供应商ID", example = "SUP001")
    @ExcelProperty("供应商ID")
    private String supplierId;

    @Schema(description = "供应商名称", example = "华东供应商")
    @ExcelProperty("供应商名称")
    private String supplierName;

    @Schema(description = "收货仓库ID", example = "W001")
    @ExcelProperty("收货仓库ID")
    private String warehouseId;

    @Schema(description = "收货仓库名称", example = "华东一仓")
    @ExcelProperty("收货仓库名称")
    private String warehouseName;

    @Schema(description = "采购日期")
    @ExcelProperty("采购日期")
    private LocalDate purchaseDate;

    @Schema(description = "采购单状态", example = "1")
    @ExcelProperty("采购单状态")
    private String orderStatus;

    @Schema(description = "收货状态", example = "1")
    @ExcelProperty("收货状态")
    private String receiveStatus;

    @Schema(description = "总商品量", example = "10")
    @ExcelProperty("总商品量")
    private Integer totalQty;

    @Schema(description = "总金额", example = "125.00")
    @ExcelProperty("总金额")
    private BigDecimal totalAmount;

    @Schema(description = "总入库数", example = "10")
    @ExcelProperty("总入库数")
    private Integer totalInboundQty;

    @Schema(description = "差异数", example = "0")
    @ExcelProperty("差异数")
    private Integer diffQty;

    @Schema(description = "退货数", example = "0")
    @ExcelProperty("退货数")
    private Integer returnQty;

    @Schema(description = "采购员", example = "张三")
    @ExcelProperty("采购员")
    private String purchaser;

    @Schema(description = "收货地址", example = "上海市浦东新区XX路1号")
    @ExcelProperty("收货地址")
    private String receiveAddress;

    @Schema(description = "审核日期")
    @ExcelProperty("审核日期")
    private LocalDate auditDate;

    @Schema(description = "备注", example = "加急采购")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "商品名称汇总")
    @ExcelProperty("商品名称汇总")
    private String productNames;

    @Schema(description = "采购单明细列表")
    private List<Item> items;

    @Schema(description = "管理后台 - 仓库采购订单明细 Response VO")
    @Data
    public static class Item {

        @Schema(description = "明细ID", example = "1")
        private Long detailId;

        @Schema(description = "采购订单ID", example = "1")
        private Long purchaseOrderId;

        @Schema(description = "采购单号", example = "CGDD20260416000001")
        private String purchaseOrderNo;

        @Schema(description = "商品SKU ID", example = "1")
        private Long productSkuId;

        @Schema(description = "商品编码", example = "SKU001")
        private String productSkuCode;

        @Schema(description = "商品名称", example = "白色款式")
        private String productSkuName;

        @Schema(description = "采购数量", example = "10")
        private Integer purchaseQty;

        @Schema(description = "箱数", example = "1")
        private Integer boxQty;

        @Schema(description = "标准装箱数量", example = "10")
        private Integer standardBoxQty;

        @Schema(description = "采购单价", example = "12.50")
        private BigDecimal purchasePrice;

        @Schema(description = "采购金额", example = "125.00")
        private BigDecimal purchaseAmount;

        @Schema(description = "已入库数量", example = "10")
        private Integer inboundQty;

        @Schema(description = "退货数", example = "0")
        private Integer returnQty;

        @Schema(description = "差异数", example = "0")
        private Integer diffQty;
    }

}
