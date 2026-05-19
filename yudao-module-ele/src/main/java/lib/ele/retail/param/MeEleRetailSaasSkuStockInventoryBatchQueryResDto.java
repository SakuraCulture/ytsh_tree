package lib.ele.retail.param;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class MeEleRetailSaasSkuStockInventoryBatchQueryResDto {

    @JSONField(name = "merchantCode")
    private String merchant_code;
    @JSONField(name = "erpStoreCode")
    private String erp_store_code;
    @JSONField(name = "skuInventoryDTOList", alternateNames = {"inventoryList"})
    private ErpSkuInventoryResultDTO[] inventory_list;
}
