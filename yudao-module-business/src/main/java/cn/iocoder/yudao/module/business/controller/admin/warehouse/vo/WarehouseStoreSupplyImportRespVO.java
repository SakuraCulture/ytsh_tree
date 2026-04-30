package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "仓库门店供货关系导入结果 VO")
public class WarehouseStoreSupplyImportRespVO {

    @Schema(description = "创建成功的数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer createCount;

    @Schema(description = "更新成功的数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Integer updateCount;

    @Schema(description = "导入失败的数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer failureCount;

    @Schema(description = "失败行和原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> failureRows;
}
