package com.alibaba.ocean.rawsdk.example.param;

import com.alibaba.ocean.rawsdk.client.APIId;
import com.alibaba.ocean.rawsdk.common.AbstractAPIRequest;

public class CommonBusinessCatParam extends AbstractAPIRequest<CommonBusinessCatResult> {

    private CBCRequestBody body;

    public CommonBusinessCatParam() {
        super();
        oceanApiId = new APIId("me.ele.retail", "common.businesscategories", 3);
    }

    
    public CBCRequestBody getBody() {
        return body;
    }

    
    public void setBody(CBCRequestBody body) {
        this.body = body;
    }

}
