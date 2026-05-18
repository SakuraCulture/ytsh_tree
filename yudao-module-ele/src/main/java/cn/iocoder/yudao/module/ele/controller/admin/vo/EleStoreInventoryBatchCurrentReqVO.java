package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 饿了么当前门店库存批量任务 Request VO")
@Data
public class EleStoreInventoryBatchCurrentReqVO {

    @Schema(description = "商家编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "merchant-1")
    @NotBlank(message = "merchantCode不能为空")
    private String merchantCode;

    @Schema(description = "ERP 门店编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "10001")
    @NotBlank(message = "erpStoreCode不能为空")
    private String erpStoreCode;

    @Schema(description = "平台门店编码", example = "ELE_STORE_001")
    private String platformStoreId;
}
