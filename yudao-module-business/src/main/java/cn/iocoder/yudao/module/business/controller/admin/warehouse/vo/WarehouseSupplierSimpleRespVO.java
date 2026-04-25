package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 仓库供应商简单 Response VO")
@Data
public class WarehouseSupplierSimpleRespVO {

    @Schema(description = "供应商ID", example = "SUP001")
    private String supplierId;

    @Schema(description = "供应商名称", example = "示例供应商")
    private String supplierName;

}
