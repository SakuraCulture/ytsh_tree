package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 门店商品批量标签保存 Response VO")
@Data
public class StoreProductTagBatchRespVO {

    @Schema(description = "成功数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer successCount;

    @Schema(description = "失败数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer failureCount;

    @Schema(description = "失败明细", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<FailureDetail> failureDetails;

    @Schema(description = "管理后台 - 门店商品批量标签失败明细")
    @Data
    public static class FailureDetail {

        @Schema(description = "门店商品 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "SP-001")
        private String objectId;

        @Schema(description = "失败原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "门店商品不存在")
        private String reason;

    }

}
