package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
@Schema(description = "管理后台 - 饿了么门店商品同步 Request VO")
public class EleStoreGoodsSyncReqVO {

    @Schema(description = "链路 ID", example = "trace-1")
    private String traceId;

    @Schema(description = "ticket", example = "ticket-1")
    private String ticket;

    @Schema(description = "接口编码", example = "GOODS_NOTIFY")
    private String apiCode;

    @Schema(description = "接口名称", example = "商品同步通知")
    private String apiName;

    @Schema(description = "商家编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "merchant-1")
    @NotBlank(message = "merchantCode不能为空")
    private String merchantCode;

    @Schema(description = "ERP 门店编码", example = "10001")
    private String erpStoreCode;

    @Schema(description = "平台门店编码", example = "10001")
    private String platformStoreId;

    @Schema(description = "三方 SKU 编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "sku-1")
    @NotBlank(message = "skuCode不能为空")
    private String skuCode;

    @Schema(description = "子 SKU 编码", example = "sub-sku-1")
    private String subSkuCode;

    @Schema(description = "SPU 编码", example = "spu-1")
    private String spuCode;

    @Schema(description = "商品层级", example = "SKU")
    private String goodsLevel;

    @Schema(description = "操作类型", example = "CREATE")
    private String operationType;

    @Schema(description = "门店商品 POS 状态", example = "上架")
    private String storeProductPosStatus;

    @Schema(description = "门店商品价格", example = "12.50")
    private BigDecimal storeProductPrice;

    @Schema(description = "门店商品启用状态", example = "1")
    private Integer storeProductIsActive;

    @Schema(description = "页码", example = "1")
    private Integer pageNo;

    @Schema(description = "每页条数", example = "100")
    private Integer pageSize;

    @Schema(description = "数据条数", example = "1")
    private Integer dataCount;

    @Schema(description = "请求报文")
    private String requestBody;

    @Schema(description = "响应报文")
    private String responseBody;

    @Schema(description = "原始负载")
    private String rawPayload;

    @Schema(description = "是否测试模式", example = "false")
    private Boolean testMode;
}
