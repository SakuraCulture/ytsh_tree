package lib.ele.retail.param;

import lombok.Data;

@Data
public class SaasGoodsStoreQueryBatchResult {

    private String errno;
    private String error;
    private MeEleRetailSaasGoodsStoreQueryBatchResDto data;
}
