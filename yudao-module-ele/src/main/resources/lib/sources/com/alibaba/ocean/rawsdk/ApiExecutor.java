package com.alibaba.ocean.rawsdk;

import com.alibaba.fastjson.JSON;
import com.alibaba.ocean.rawsdk.client.Response;
import com.alibaba.ocean.rawsdk.client.http.AbstractHttpClient;
import com.alibaba.ocean.rawsdk.client.http.HttpSupport;
import com.alibaba.ocean.rawsdk.client.pollicy.RequestPolicy;
import com.alibaba.ocean.rawsdk.common.AbstractAPIRequest;
import com.alibaba.ocean.rawsdk.common.BizResultWrapper;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

public final class ApiExecutor<TResult> {
    private static Logger logger = Logger.getLogger("ApiExecutor");

    private volatile AbstractHttpClient httpClient;
    private String appKey;
    private String secKey;


    public ApiExecutor(String appkey, String secKey) {
        super();
        this.appKey = appkey;
        this.secKey = secKey;
    }

    
    public final <TResult> BizResultWrapper<TResult> send(AbstractAPIRequest<TResult> request) throws IOException {
                Map<String, Object> parameters = buildHttpParams(request);
                RequestPolicy policy = buildHttpPolicy(request);
                buildHttpSignature(parameters, policy);
                URL postURL = buildUrl(policy);
                        Response response = doAction(request, parameters, postURL);
                        if (response.getException() != null) {
            Throwable responseException = response.getException();
            throw new RuntimeException(responseException);
        }
        return (BizResultWrapper<TResult>) response.getResult();
    }

    private <TResult> Response doAction(AbstractAPIRequest<TResult> request, Map<String, Object> parameters, URL postURL) throws IOException {
        Response response = getHttpClient().doPost(postURL, parameters, request);
        return response;
    }

    private URL buildUrl(RequestPolicy policy) throws IOException {
        URL postURL = HttpSupport.buildPostRequestUrl(policy);
        return postURL;
    }

    private void buildHttpSignature(Map<String, Object> parameters, RequestPolicy policy) {
        HttpSupport.signature(parameters, this.appKey, this.secKey, policy);
    }

    private <TResult> RequestPolicy buildHttpPolicy(AbstractAPIRequest<TResult> request) {
        RequestPolicy policy = request.getOceanRequestPolicy();
        return policy;
    }

    private <TResult> Map<String, Object> buildHttpParams(AbstractAPIRequest<TResult> request) {
        Map<String, Object> parameters = HttpSupport.buildParams(request, this.appKey);
        return parameters;
    }

    public AbstractHttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = new AbstractHttpClient();
                }
            }
        }
        return httpClient;
    }

    public void setHttpClient(AbstractHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getSecKey() {
        return secKey;
    }

    public void setSecKey(String secKey) {
        this.secKey = secKey;
    }
}
