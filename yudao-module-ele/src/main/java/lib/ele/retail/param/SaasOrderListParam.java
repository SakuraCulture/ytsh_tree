package lib.ele.retail.param;

import com.alibaba.ocean.rawsdk.client.APIId;
import com.alibaba.ocean.rawsdk.common.AbstractAPIRequest;

import java.util.*;
import java.math.BigDecimal;
import java.math.BigInteger;

public class SaasOrderListParam extends AbstractAPIRequest<SaasOrderListResult> {

    private MeEleRetailSaasOrderListReqDto body;

    public SaasOrderListParam() {
        super();
        oceanApiId = new APIId("me.ele.retail", "saas.order.list", 3);
    }

    
    public MeEleRetailSaasOrderListReqDto getBody() {
        return body;
    }

    
    public void setBody(MeEleRetailSaasOrderListReqDto body) {
        this.body = body;
    }

}
