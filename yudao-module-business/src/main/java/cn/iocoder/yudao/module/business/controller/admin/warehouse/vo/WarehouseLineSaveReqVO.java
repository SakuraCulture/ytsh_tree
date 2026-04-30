package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 线路新增/修改 Request VO")
@Data
public class WarehouseLineSaveReqVO {

    @Schema(description = "线路ID")
    private Long lineId;

    @Schema(description = "仓库ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "仓库不能为空")
    private String warehouseId;

    @Schema(description = "线路编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "线路编码不能为空")
    private String lineCode;

    @Schema(description = "线路名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "线路名称不能为空")
    private String lineName;

    @Schema(description = "下单星期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "下单星期不能为空")
    private List<Integer> orderWeekdays;

    @Schema(description = "线路状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "线路状态不能为空")
    private Integer lineStatus;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "参与门店ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "参与门店列表不能为空")
    private List<String> storeIds;
}
