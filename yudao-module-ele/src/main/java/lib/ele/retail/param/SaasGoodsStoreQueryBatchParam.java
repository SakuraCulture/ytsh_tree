package lib.ele.retail.param;

import com.alibaba.ocean.rawsdk.client.APIId;
import com.alibaba.ocean.rawsdk.common.AbstractAPIRequest;

public class SaasGoodsStoreQueryBatchParam extends AbstractAPIRequest<SaasGoodsStoreQueryBatchResult> {

    private MeEleRetailSaasGoodsStoreQueryBatchReqDto body;

    public SaasGoodsStoreQueryBatchParam() {
        super();
        oceanApiId = new APIId("me.ele.retail", "saas.goods.store.query.batch", 3);
    }

    public MeEleRetailSaasGoodsStoreQueryBatchReqDto getBody() {
        return body;
    }

    public void setBody(MeEleRetailSaasGoodsStoreQueryBatchReqDto body) {
        this.body = body;
    }
}
