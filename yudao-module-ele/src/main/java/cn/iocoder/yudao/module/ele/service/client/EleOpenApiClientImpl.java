package cn.iocoder.yudao.module.ele.service.client;

import cn.iocoder.yudao.module.business.integration.apilog.service.ExternalApiCallLogService;
import cn.iocoder.yudao.module.business.integration.apilog.service.bo.ExternalApiCallLogCreateReqBO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleApiConfig;
import cn.iocoder.yudao.module.ele.service.traffic.EleTrafficInterceptor;
import com.alibaba.ocean.rawsdk.ApiExecutor;
import com.alibaba.ocean.rawsdk.common.BizResultWrapper;
import jakarta.annotation.Resource;
import lib.ele.retail.param.SaasGoodsStoreQueryBatchParam;
import lib.ele.retail.param.SaasGoodsStoreQueryBatchResult;
import lib.ele.retail.param.SaasOrderGetParam;
import lib.ele.retail.param.SaasOrderGetResult;
import lib.ele.retail.param.SaasOrderListParam;
import lib.ele.retail.param.SaasOrderListResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EleOpenApiClientImpl implements EleOpenApiClient {

    private static final String PLATFORM_CODE = "ELE";
    private static final String ORDER_LIST_API_CODE = "ORDER_LIST";
    private static final String ORDER_DETAIL_API_CODE = "ORDER_DETAIL";
    private static final String STORE_GOODS_QUERY_BATCH_API_CODE = "STORE_GOODS_QUERY_BATCH";

    @Resource
    private ExternalApiCallLogService externalApiCallLogService;

    @Resource
    private EleTrafficInterceptor trafficInterceptor;

    @Value("${yudao.ele.api-log.log-success-enabled:false}")
    private boolean logSuccessEnabled;

    @Override
    public BizResultWrapper<SaasOrderListResult> sendOrderList(EleApiConfig config, SaasOrderListParam param,
                                                               String merchantCode, String platformStoreId,
                                                               String erpStoreCode) {
        long startTime = System.currentTimeMillis();
        String traceId = null;
        try {
            trafficInterceptor.beforeRequest(ORDER_LIST_API_CODE, param != null ? param.getTicket() : null, param);
            BizResultWrapper<SaasOrderListResult> wrapper = buildListExecutor(config).send(param);
            traceId = wrapper == null ? null : wrapper.getTraceid();
            boolean success = !isFailure(wrapper);
            trafficInterceptor.afterResponse(ORDER_LIST_API_CODE, traceId, wrapper, success,
                    (int) (System.currentTimeMillis() - startTime));
            if (shouldRecordSuccess(wrapper)) {
                recordLog(ORDER_LIST_API_CODE, "订单列表", traceId, param, wrapper,
                        merchantCode, platformStoreId, erpStoreCode, null, null, true, resultCode(wrapper), resultMsg(wrapper),
                        (int) (System.currentTimeMillis() - startTime));
            } else if (isFailure(wrapper)) {
                recordLog(ORDER_LIST_API_CODE, "订单列表", traceId, param, wrapper,
                        merchantCode, platformStoreId, erpStoreCode, null, null, false, resultCode(wrapper), resultMsg(wrapper),
                        (int) (System.currentTimeMillis() - startTime));
            }
            return wrapper;
        } catch (Exception e) {
            trafficInterceptor.afterResponse(ORDER_LIST_API_CODE, traceId, null, false,
                    (int) (System.currentTimeMillis() - startTime));
            recordLog(ORDER_LIST_API_CODE, "订单列表", null, param, null,
                    merchantCode, platformStoreId, erpStoreCode, null, null, false, null, e.getMessage(),
                    (int) (System.currentTimeMillis() - startTime));
            throw new RuntimeException(e);
        }
    }

    @Override
    public BizResultWrapper<SaasOrderGetResult> sendOrderDetail(EleApiConfig config, SaasOrderGetParam param,
                                                                String merchantCode, String platformStoreId,
                                                                String erpStoreCode, String orderId) {
        long startTime = System.currentTimeMillis();
        String traceId = null;
        try {
            trafficInterceptor.beforeRequest(ORDER_DETAIL_API_CODE, param != null ? param.getTicket() : null, param);
            BizResultWrapper<SaasOrderGetResult> wrapper = buildDetailExecutor(config).send(param);
            traceId = wrapper == null ? null : wrapper.getTraceid();
            boolean success = !isFailure(wrapper);
            trafficInterceptor.afterResponse(ORDER_DETAIL_API_CODE, traceId, wrapper, success,
                    (int) (System.currentTimeMillis() - startTime));
            if (shouldRecordSuccess(wrapper)) {
                recordLog(ORDER_DETAIL_API_CODE, "订单详情", traceId, param, wrapper,
                        merchantCode, platformStoreId, erpStoreCode, orderId, null, true, resultCode(wrapper), resultMsg(wrapper),
                        (int) (System.currentTimeMillis() - startTime));
            } else if (isFailure(wrapper)) {
                recordLog(ORDER_DETAIL_API_CODE, "订单详情", traceId, param, wrapper,
                        merchantCode, platformStoreId, erpStoreCode, orderId, null, false, resultCode(wrapper), resultMsg(wrapper),
                        (int) (System.currentTimeMillis() - startTime));
            }
            return wrapper;
        } catch (Exception e) {
            trafficInterceptor.afterResponse(ORDER_DETAIL_API_CODE, traceId, null, false,
                    (int) (System.currentTimeMillis() - startTime));
            recordLog(ORDER_DETAIL_API_CODE, "订单详情", null, param, null,
                    merchantCode, platformStoreId, erpStoreCode, orderId, null, false, null, e.getMessage(),
                    (int) (System.currentTimeMillis() - startTime));
            throw new RuntimeException(e);
        }
    }

    @Override
    public BizResultWrapper<SaasGoodsStoreQueryBatchResult> sendStoreGoodsQueryBatch(EleApiConfig config,
                                                                                     SaasGoodsStoreQueryBatchParam param,
                                                                                     String merchantCode,
                                                                                     String platformStoreId,
                                                                                     String erpStoreCode) {
        long startTime = System.currentTimeMillis();
        String traceId = null;
        try {
            trafficInterceptor.beforeRequest(STORE_GOODS_QUERY_BATCH_API_CODE, param != null ? param.getTicket() : null, param);
            BizResultWrapper<SaasGoodsStoreQueryBatchResult> wrapper = buildStoreGoodsQueryExecutor(config).send(param);
            traceId = wrapper == null ? null : wrapper.getTraceid();
            boolean success = !isFailure(wrapper);
            trafficInterceptor.afterResponse(STORE_GOODS_QUERY_BATCH_API_CODE, traceId, wrapper, success,
                    (int) (System.currentTimeMillis() - startTime));
            if (shouldRecordSuccess(wrapper)) {
                recordLog(STORE_GOODS_QUERY_BATCH_API_CODE, "门店商品批量查询", traceId, param, wrapper,
                        merchantCode, platformStoreId, erpStoreCode, null, null, true, resultCode(wrapper), resultMsg(wrapper),
                        (int) (System.currentTimeMillis() - startTime));
            } else if (isFailure(wrapper)) {
                recordLog(STORE_GOODS_QUERY_BATCH_API_CODE, "门店商品批量查询", traceId, param, wrapper,
                        merchantCode, platformStoreId, erpStoreCode, null, null, false, resultCode(wrapper), resultMsg(wrapper),
                        (int) (System.currentTimeMillis() - startTime));
            }
            return wrapper;
        } catch (Exception e) {
            trafficInterceptor.afterResponse(STORE_GOODS_QUERY_BATCH_API_CODE, traceId, null, false,
                    (int) (System.currentTimeMillis() - startTime));
            recordLog(STORE_GOODS_QUERY_BATCH_API_CODE, "门店商品批量查询", null, param, null,
                    merchantCode, platformStoreId, erpStoreCode, null, null, false, null, e.getMessage(),
                    (int) (System.currentTimeMillis() - startTime));
            throw new RuntimeException(e);
        }
    }

    protected ApiExecutor<SaasOrderListResult> buildListExecutor(EleApiConfig config) {
        return new ApiExecutor<>(config.getAppId(), config.getAppSecret());
    }

    protected ApiExecutor<SaasOrderGetResult> buildDetailExecutor(EleApiConfig config) {
        return new ApiExecutor<>(config.getAppId(), config.getAppSecret());
    }

    protected ApiExecutor<SaasGoodsStoreQueryBatchResult> buildStoreGoodsQueryExecutor(EleApiConfig config) {
        return new ApiExecutor<>(config.getAppId(), config.getAppSecret());
    }

    private void recordLog(String apiCode, String apiName, String externalTraceId, Object request, Object response,
                           String merchantCode, String platformStoreId, String erpStoreCode,
                           String orderId, String channelOrderId, boolean success,
                           String resultCode, String resultMsg, Integer durationMs) {
        ExternalApiCallLogCreateReqBO reqBO = new ExternalApiCallLogCreateReqBO();
        reqBO.setPlatformCode(PLATFORM_CODE);
        reqBO.setApiCode(apiCode);
        reqBO.setApiName(apiName);
        reqBO.setBizType("ORDER");
        reqBO.setBizId(orderId);
        reqBO.setBizNo(channelOrderId != null ? channelOrderId : orderId);
        reqBO.setExternalTraceId(externalTraceId);
        reqBO.setRequestMethod("POST");
        reqBO.setRequestBody(request == null ? null : String.valueOf(request));
        reqBO.setResponseBody(response == null ? null : String.valueOf(response));
        reqBO.setSuccess(success);
        reqBO.setResultCode(resultCode);
        reqBO.setResultMsg(resultMsg);
        reqBO.setDurationMs(durationMs);
        reqBO.setMerchantCode(merchantCode);
        reqBO.setPlatformStoreId(platformStoreId);
        reqBO.setErpStoreCode(erpStoreCode);
        reqBO.setOrderId(orderId);
        reqBO.setChannelOrderId(channelOrderId);
        externalApiCallLogService.createExternalApiCallLog(reqBO);
    }

    private boolean shouldRecordSuccess(BizResultWrapper<?> wrapper) {
        return logSuccessEnabled && !isFailure(wrapper);
    }

    private boolean isFailure(BizResultWrapper<?> wrapper) {
        String code = resultCode(wrapper);
        return code != null && !"0".equals(code);
    }

    private String resultCode(BizResultWrapper<?> wrapper) {
        Object body = wrapper == null ? null : wrapper.getBody();
        if (body instanceof SaasOrderListResult result) {
            return result.getErrno();
        }
        if (body instanceof SaasOrderGetResult result) {
            return result.getErrno();
        }
        if (body instanceof SaasGoodsStoreQueryBatchResult result) {
            return result.getErrno();
        }
        return null;
    }

    private String resultMsg(BizResultWrapper<?> wrapper) {
        Object body = wrapper == null ? null : wrapper.getBody();
        if (body instanceof SaasOrderListResult result) {
            return result.getError();
        }
        if (body instanceof SaasOrderGetResult result) {
            return result.getError();
        }
        if (body instanceof SaasGoodsStoreQueryBatchResult result) {
            return result.getError();
        }
        return null;
    }
}
