package lib.ele.retail.param;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class MeEleRetailSaasSkuStockInventoryBatchQueryReqDto {

    @JSONField(name = "merchantCode")
    private String merchant_code;
    @JSONField(name = "erpStoreCode")
    private String erp_store_code;
    @JSONField(name = "skuCodes")
    private String[] sku_code_list;
}
