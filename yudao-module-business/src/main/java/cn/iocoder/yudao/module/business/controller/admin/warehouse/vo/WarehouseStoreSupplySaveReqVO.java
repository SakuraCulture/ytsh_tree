package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 仓库门店供货关系新增/修改 Request VO")
@Data
public class WarehouseStoreSupplySaveReqVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "仓库ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "仓库不能为空")
    private String warehouseId;

    @Schema(description = "门店ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "门店不能为空")
    private String storeId;

    @Schema(description = "是否主仓", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "主仓标记不能为空")
    private Integer isPrimary;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer supplyStatus;

    @Schema(description = "备注")
    private String remark;
}
