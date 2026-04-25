package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 仓库简单 Response VO")
@Data
public class WarehouseSimpleRespVO {

    @Schema(description = "仓库ID", example = "W001")
    private String warehouseId;

    @Schema(description = "仓库名称", example = "华东一仓")
    private String warehouseName;

    @Schema(description = "是否默认仓(0否1是)", example = "0")
    private Integer isDefault;

}
