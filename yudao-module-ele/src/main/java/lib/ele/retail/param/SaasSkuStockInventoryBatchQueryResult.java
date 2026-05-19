package lib.ele.retail.param;

import lombok.Data;

@Data
public class SaasSkuStockInventoryBatchQueryResult {

    private String errno;
    private String error;
    private MeEleRetailSaasSkuStockInventoryBatchQueryResDto data;
}
