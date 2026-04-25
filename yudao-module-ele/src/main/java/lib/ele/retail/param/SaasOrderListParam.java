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

    /**
     * @return 请求参数
     */
    public MeEleRetailSaasOrderListReqDto getBody() {
        return body;
    }

    /**
     * 设置请求参数     *
     * 参数示例�?pre>见消息体</pre>
     * 此参数必�?
     */
    public void setBody(MeEleRetailSaasOrderListReqDto body) {
        this.body = body;
    }

}
