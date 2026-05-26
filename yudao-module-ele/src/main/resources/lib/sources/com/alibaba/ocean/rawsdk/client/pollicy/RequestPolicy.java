package com.alibaba.ocean.rawsdk.client.pollicy;

import com.alibaba.ocean.rawsdk.common.EleConstants;

public class RequestPolicy implements Cloneable {

    private boolean requestSendTimestamp = false;
    private boolean useHttps = true;
    private Protocol requestProtocol = Protocol.v3;
    private Protocol responseProtocol = Protocol.json2;
    private boolean responseCompress = true;
    private int requestCompressThreshold = -1;     private int timeout = 30000;
    private HttpMethodPolicy httpMethod = HttpMethodPolicy.POST;
    private String queryStringCharset = "GB18030";         private String contentCharset = "UTF-8";     private boolean useSignture = true;
        private boolean accessPrivateApi = false;
    private int defaultApiVersion = 1;
    private String dateFormat = "yyyy-MM-dd HH:mm:ss.SSS";
    private String agent = "Ocean-SDK-Client";

    private String serverHost = EleConstants.DEFAULT_HOST;

    private int httpPort = EleConstants.DEFAULT_HTTP_PORT;
    private int httpsPort = EleConstants.DEFAULT_HTTPS_PORT;

    public RequestPolicy clone() {
        RequestPolicy newObj = newPolicy();
        newObj.requestSendTimestamp = requestSendTimestamp;
        newObj.useHttps = useHttps;
        newObj.requestProtocol = requestProtocol;
        newObj.responseProtocol = responseProtocol;
        newObj.responseCompress = responseCompress;
        newObj.requestCompressThreshold = requestCompressThreshold;
        newObj.timeout = timeout;
        newObj.httpMethod = httpMethod;
        newObj.queryStringCharset = queryStringCharset;
        newObj.contentCharset = contentCharset;
        newObj.useSignture = useSignture;
        newObj.accessPrivateApi = accessPrivateApi;
        newObj.defaultApiVersion = defaultApiVersion;
        return newObj;
    }

    protected RequestPolicy newPolicy() {
        return new RequestPolicy();
    }

    public boolean isRequestSendTimestamp() {
        return requestSendTimestamp;
    }

    
    public RequestPolicy setRequestSendTimestamp(boolean requestSendTimestamp) {
        this.requestSendTimestamp = requestSendTimestamp;
        return this;
    }

    public boolean isUseHttps() {
        return useHttps;
    }

    
    public RequestPolicy setUseHttps(boolean useHttps) {
        this.useHttps = useHttps;
        return this;
    }

    public Protocol getRequestProtocol() {
        return requestProtocol;
    }

    
    public RequestPolicy setRequestProtocol(Protocol protocol) {
        if (protocol == null) {
            throw new IllegalArgumentException("protocol can not be null");
        }
        this.requestProtocol = protocol;
        return this;
    }

    public boolean isResponseCompress() {
        return responseCompress;
    }

    
    private RequestPolicy setResponseCompress(boolean responseCompress) {
        this.responseCompress = responseCompress;
        return this;
    }

    public int getRequestCompressThreshold() {
        return requestCompressThreshold;
    }

    
    public RequestPolicy setRequestCompressThreshold(int requestCompressThreshold) {
        this.requestCompressThreshold = requestCompressThreshold;
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    
    public RequestPolicy setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public HttpMethodPolicy getHttpMethod() {
        return httpMethod;
    }

    
    public RequestPolicy setHttpMethod(HttpMethodPolicy httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public String getQueryStringCharset() {
        return queryStringCharset;
    }

    
    public RequestPolicy setQueryStringCharset(String queryStringCharset) {
        this.queryStringCharset = queryStringCharset;
        return this;
    }

    public String getContentCharset() {
        return contentCharset;
    }

    
    public RequestPolicy setContentCharset(String contentCharset) {
        this.contentCharset = contentCharset;
        return this;
    }

    public Protocol getResponseProtocol() {
        return responseProtocol;
    }

    
    public RequestPolicy setResponseProtocol(Protocol responseProtocol) {
        if (requestProtocol == null) {
            throw new IllegalArgumentException("response protocol can not be null");
        }
        this.responseProtocol = responseProtocol;
        return this;
    }

    public boolean isUseSignture() {
        return useSignture;
    }

    
    public RequestPolicy setUseSignture(boolean useSignture) {
        this.useSignture = useSignture;
        return this;
    }

    public boolean isAccessPrivateApi() {
        return accessPrivateApi;
    }

    
    public RequestPolicy setAccessPrivateApi(boolean accessPrivateApi) {
        this.accessPrivateApi = accessPrivateApi;
        return this;
    }

    public int getDefaultApiVersion() {
        return defaultApiVersion;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public RequestPolicy setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
        return this;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    
    public static enum HttpMethodPolicy {
        
        POST,
        
        GET;
    }
}
