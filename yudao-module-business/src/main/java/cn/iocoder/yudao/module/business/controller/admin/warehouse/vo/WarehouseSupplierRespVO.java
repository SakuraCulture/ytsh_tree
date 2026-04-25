package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 仓库供应商 Response VO")
@Data
@ExcelIgnoreUnannotated
public class WarehouseSupplierRespVO {

    @Schema(description = "供应商ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "SUP001")
    @ExcelProperty("供应商ID")
    private String supplierId;

    @Schema(description = "供应商名称", example = "示例供应商")
    @ExcelProperty("供应商名称")
    private String supplierName;

    @Schema(description = "供应商分类", example = "食品")
    @ExcelProperty("供应商分类")
    private String categoryName;

    @Schema(description = "负责人", example = "张三")
    @ExcelProperty("负责人")
    private String managerName;

    @Schema(description = "电话", example = "13800000000")
    @ExcelProperty("电话")
    private String phone;

    @Schema(description = "联系地址")
    @ExcelProperty("联系地址")
    private String address;

    @Schema(description = "付款方式", example = "月结")
    @ExcelProperty("付款方式")
    private String paymentMethod;

    @Schema(description = "账期天数", example = "30")
    @ExcelProperty("账期天数")
    private Integer paymentDays;

    @Schema(description = "供应商状态(0停用1正常)", example = "1")
    @ExcelProperty("供应商状态")
    private Integer supplierStatus;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
