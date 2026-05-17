package cn.iocoder.yudao.module.business.controller.admin.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 商品 SPU 批量标签保存 Response VO")
@Data
public class ProductSpuTagBatchRespVO {

    @Schema(description = "成功数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer successCount;

    @Schema(description = "失败数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer failureCount;

    @Schema(description = "失败明细", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<FailureDetail> failureDetails;

    @Schema(description = "管理后台 - 商品 SPU 批量标签失败明细")
    @Data
    public static class FailureDetail {

        @Schema(description = "商品 SPU ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
        private String objectId;

        @Schema(description = "失败原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "商品 SPU 不存在")
        private String reason;

    }

}
