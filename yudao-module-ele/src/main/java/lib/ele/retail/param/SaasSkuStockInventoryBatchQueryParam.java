package lib.ele.retail.param;

import com.alibaba.ocean.rawsdk.client.APIId;
import com.alibaba.ocean.rawsdk.common.AbstractAPIRequest;

public class SaasSkuStockInventoryBatchQueryParam extends AbstractAPIRequest<SaasSkuStockInventoryBatchQueryResult> {

    private MeEleRetailSaasSkuStockInventoryBatchQueryReqDto body;

    public SaasSkuStockInventoryBatchQueryParam() {
        super();
        oceanApiId = new APIId("me.ele.retail", "saas.sku.stock.inventoy.batch.query", 3);
    }

    public MeEleRetailSaasSkuStockInventoryBatchQueryReqDto getBody() {
        return body;
    }

    public void setBody(MeEleRetailSaasSkuStockInventoryBatchQueryReqDto body) {
        this.body = body;
    }
}
