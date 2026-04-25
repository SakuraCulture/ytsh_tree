package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 仓库供应商新增/修改 Request VO")
@Data
public class WarehouseSupplierSaveReqVO {

    @Schema(description = "供应商ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "SUP001")
    private String supplierId;

    @Schema(description = "供应商名称", example = "示例供应商")
    private String supplierName;

    @Schema(description = "供应商分类", example = "食品")
    private String categoryName;

    @Schema(description = "负责人", example = "张三")
    private String managerName;

    @Schema(description = "电话", example = "13800000000")
    private String phone;

    @Schema(description = "联系地址")
    private String address;

    @Schema(description = "付款方式", example = "月结")
    private String paymentMethod;

    @Schema(description = "账期天数", example = "30")
    private Integer paymentDays;

    @Schema(description = "供应商状态(0停用1正常)", example = "1")
    private Integer supplierStatus;

}
