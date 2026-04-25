package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
@Schema(description = "管理后台 - 饿了么门店商品批量查询 Request VO")
public class EleStoreGoodsQueryReqVO {

    @Schema(description = "商家编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "merchant-1")
    @NotBlank(message = "merchantCode不能为空")
    private String merchantCode;

    @Schema(description = "ERP 门店编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "10001")
    @NotBlank(message = "erpStoreCode不能为空")
    private String erpStoreCode;

    @Schema(description = "SKU 编码列表")
    private List<String> skuCodeList;

    @Schema(description = "页码", example = "1")
    private Integer pageNo;

    @Schema(description = "每页条数", example = "20")
    private Integer pageSize;
}
