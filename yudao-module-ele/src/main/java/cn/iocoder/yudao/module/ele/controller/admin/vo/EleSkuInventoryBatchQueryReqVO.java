package cn.iocoder.yudao.module.ele.controller.admin.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "管理后台 - 饿了么门店库存批量查询 Request VO")
public class EleSkuInventoryBatchQueryReqVO {

    @Schema(description = "平台门店编码", example = "ELE_STORE_001")
    private String platformStoreId;

    @Schema(description = "商家编码", example = "merchant-001")
    private String merchantCode;

    @Schema(description = "ERP 门店编码", example = "ERP_STORE_001")
    private String erpStoreCode;

    @Schema(description = "SKU 编码列表")
    private List<String> skuCodes;

    @JsonIgnore
    @AssertTrue(message = "platformStoreId不能为空，或 merchantCode 和 erpStoreCode 不能为空")
    public boolean isStoreLocatorValid() {
        return hasText(platformStoreId) || (hasText(merchantCode) && hasText(erpStoreCode));
    }

    @JsonIgnore
    @AssertTrue(message = "skuCodes 不能为空")
    public boolean isSkuKeysValid() {
        return hasAnyText(skuCodes);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean hasAnyText(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return false;
        }
        for (String code : codes) {
            if (hasText(code)) {
                return true;
            }
        }
        return false;
    }
}
