package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 仓库新增/修改 Request VO")
@Data
public class WarehouseSaveReqVO {

    @Schema(description = "仓库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "W001")
    private String warehouseId;

    @Schema(description = "仓库编码", example = "WH001")
    private String warehouseCode;

    @Schema(description = "仓库名称", example = "华东一仓")
    private String warehouseName;

    @Schema(description = "仓库类型", example = "成品仓")
    private String warehouseType;

    @Schema(description = "行政区划代码")
    private String regionCode;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "仓库状态(0停用1正常)", example = "1")
    private Integer warehouseStatus;

    @Schema(description = "是否默认仓(0否1是)", example = "0")
    private Integer isDefault;

}
