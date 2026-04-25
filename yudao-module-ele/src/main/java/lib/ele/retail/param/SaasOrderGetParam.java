package lib.ele.retail.param;

import com.alibaba.ocean.rawsdk.client.APIId;
import com.alibaba.ocean.rawsdk.common.AbstractAPIRequest;

public class SaasOrderGetParam extends AbstractAPIRequest<SaasOrderGetResult> {

    private SaasOrderGetBody body;

    public SaasOrderGetParam() {
        super();
        oceanApiId = new APIId("me.ele.retail", "saas.order.get", 3);
    }

    public SaasOrderGetBody getBody() {
        return body;
    }

    public void setBody(SaasOrderGetBody body) {
        this.body = body;
    }

    public static class SaasOrderGetBody {
        private String order_id;
        private String merchant_code;
        private String erp_store_code;
        private String market_user_id;

        public String getOrder_id() {
            return order_id;
        }

        public void setOrder_id(String order_id) {
            this.order_id = order_id;
        }

        public String getMerchant_code() {
            return merchant_code;
        }

        public void setMerchant_code(String merchant_code) {
            this.merchant_code = merchant_code;
        }

        public String getErp_store_code() {
            return erp_store_code;
        }

        public void setErp_store_code(String erp_store_code) {
            this.erp_store_code = erp_store_code;
        }

        public String getMarket_user_id() {
            return market_user_id;
        }

        public void setMarket_user_id(String market_user_id) {
            this.market_user_id = market_user_id;
        }
    }
}
