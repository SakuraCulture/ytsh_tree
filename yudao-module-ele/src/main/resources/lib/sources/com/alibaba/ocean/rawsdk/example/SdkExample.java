package com.alibaba.ocean.rawsdk.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.ocean.rawsdk.ApiExecutor;
import com.alibaba.ocean.rawsdk.common.BizResultWrapper;
import com.alibaba.ocean.rawsdk.example.param.CBCRequestBody;
import com.alibaba.ocean.rawsdk.example.param.CommonBusinessCatParam;
import com.alibaba.ocean.rawsdk.example.param.CommonBusinessCatResult;

import java.util.UUID;


public class SdkExample {

    private static final String SUCCESS_FLAG = "0";

    public static void main(String[] args) {
                String appkey = "填写自己的APP ID";
        String secKey = "填写自己的APP Secret";
        ApiExecutor apiExecutor = new ApiExecutor(appkey, secKey);

                CommonBusinessCatParam param = new CommonBusinessCatParam();
        param.setTicket(UUID.randomUUID().toString().toUpperCase());

        Integer category_id = 166;
                CBCRequestBody body = new CBCRequestBody();
        body.setCategory_id(category_id);
        param.setBody(body);

        
        try {
                        BizResultWrapper<CommonBusinessCatResult> result = apiExecutor.send(param);
            System.out.println("Result:" + JSON.toJSONString(result));
            if (null == result || null == result.getBody()) {
                System.out.println("返回数据中对象为空");
                return;
            }
            CommonBusinessCatResult commonBusinessCatResult = result.getBody();
            if (null != commonBusinessCatResult.getErrno() && SUCCESS_FLAG.equals(commonBusinessCatResult.getErrno().toString())) {
                System.out.println("请求成功");
            } else {
                System.out.println("--请求结果失败--");
            }
        } catch (Exception e) {
            System.out.println("请求失败，请求异常");
            System.out.println(e);
        }
    }
}
