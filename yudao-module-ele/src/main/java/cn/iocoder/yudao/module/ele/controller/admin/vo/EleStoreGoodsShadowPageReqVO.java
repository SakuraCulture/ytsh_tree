package cn.iocoder.yudao.module.ele.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 饿了么门店商品影子分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class EleStoreGoodsShadowPageReqVO extends PageParam {

    @Schema(description = "商家编码")
    private String merchantCode;

    @Schema(description = "ERP 门店编码")
    private String erpStoreCode;

    @Schema(description = "平台门店编码")
    private String platformStoreId;

    @Schema(description = "门店 ID")
    private String storeId;

    @Schema(description = "SKU 编码")
    private String skuCode;

    @Schema(description = "商品名称")
    private String title;

    @Schema(description = "匹配状态")
    private String matchStatus;
}
