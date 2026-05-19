package cn.iocoder.yudao.module.ele.service.client;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.integration.apilog.service.ExternalApiCallLogService;
import cn.iocoder.yudao.module.business.integration.apilog.service.bo.ExternalApiCallLogCreateReqBO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleApiConfig;
import cn.iocoder.yudao.module.ele.dal.mysql.EleApiConfigMapper;
import cn.iocoder.yudao.module.ele.service.traffic.EleTrafficInterceptor;
import cn.iocoder.yudao.module.ele.service.dto.BillListReqDTO;
import cn.iocoder.yudao.module.ele.service.dto.BillListRespDTO;
import com.alibaba.fastjson.JSON;
import com.alibaba.ocean.rawsdk.ApiExecutor;
import com.alibaba.ocean.rawsdk.common.BizResultWrapper;
import jakarta.annotation.Resource;
import lib.ele.retail.param.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EleOpenApiClientImpl implements EleOpenApiClient {

    private static final String PLATFORM_CODE = "ELE";
    private static final String ORDER_LIST_API_CODE = "ORDER_LIST";
    private static final String ORDER_DETAIL_API_CODE = "ORDER_DETAIL";
    private static final String STORE_GOODS_QUERY_BATCH_API_CODE = "STORE_GOODS_QUERY_BATCH";
    private static final String SKU_STOCK_INVENTORY_BATCH_QUERY_API_CODE = "SKU_STOCK_INVENTORY_BATCH_QUERY";
    private static final String BILL_LIST_API_CODE = "BILL_LIST";

    @Resource
    private ExternalApiCallLogService externalApiCallLogService;

    @Resource
    private EleTrafficInterceptor trafficInterceptor;

    @Resource
    private EleApiConfigMapper eleApiConfigMapper;

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

    @Override
    public BizResultWrapper<SaasSkuStockInventoryBatchQueryResult> sendSkuStockInventoryBatchQuery(EleApiConfig config,
                                                                                                    SaasSkuStockInventoryBatchQueryParam param,
                                                                                                    String merchantCode,
                                                                                                    String platformStoreId,
                                                                                                    String erpStoreCode) {
        long startTime = System.currentTimeMillis();
        String traceId = null;
        try {
            trafficInterceptor.beforeRequest(SKU_STOCK_INVENTORY_BATCH_QUERY_API_CODE, param != null ? param.getTicket() : null, param);
            BizResultWrapper<SaasSkuStockInventoryBatchQueryResult> wrapper = buildSkuStockInventoryBatchQueryExecutor(config).send(param);
            traceId = wrapper == null ? null : wrapper.getTraceid();
            boolean success = !isFailure(wrapper);
            trafficInterceptor.afterResponse(SKU_STOCK_INVENTORY_BATCH_QUERY_API_CODE, traceId, wrapper, success,
                    (int) (System.currentTimeMillis() - startTime));
            if (shouldRecordSuccess(wrapper)) {
                recordLog(SKU_STOCK_INVENTORY_BATCH_QUERY_API_CODE, "库存批量查询", traceId, param, wrapper,
                        merchantCode, platformStoreId, erpStoreCode, null, null, true, resultCode(wrapper), resultMsg(wrapper),
                        (int) (System.currentTimeMillis() - startTime));
            } else if (isFailure(wrapper)) {
                recordLog(SKU_STOCK_INVENTORY_BATCH_QUERY_API_CODE, "库存批量查询", traceId, param, wrapper,
                        merchantCode, platformStoreId, erpStoreCode, null, null, false, resultCode(wrapper), resultMsg(wrapper),
                        (int) (System.currentTimeMillis() - startTime));
            } else if (isInventoryListNull(wrapper)) {
                recordLog(SKU_STOCK_INVENTORY_BATCH_QUERY_API_CODE, "库存批量查询", traceId, param, wrapper,
                        merchantCode, platformStoreId, erpStoreCode, null, null, false, "INVENTORY_LIST_NULL", "inventory_list null",
                        (int) (System.currentTimeMillis() - startTime));
            }
            return wrapper;
        } catch (Exception e) {
            trafficInterceptor.afterResponse(SKU_STOCK_INVENTORY_BATCH_QUERY_API_CODE, traceId, null, false,
                    (int) (System.currentTimeMillis() - startTime));
            recordLog(SKU_STOCK_INVENTORY_BATCH_QUERY_API_CODE, "库存批量查询", null, param, null,
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

    protected ApiExecutor<SaasSkuStockInventoryBatchQueryResult> buildSkuStockInventoryBatchQueryExecutor(EleApiConfig config) {
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
        reqBO.setBizType(SKU_STOCK_INVENTORY_BATCH_QUERY_API_CODE.equals(apiCode) ? "INVENTORY" : "ORDER");
        reqBO.setBizId(orderId);
        reqBO.setBizNo(channelOrderId != null ? channelOrderId : orderId);
        reqBO.setExternalTraceId(externalTraceId);
        reqBO.setRequestMethod("POST");
        reqBO.setRequestBody(toLogBody(request));
        reqBO.setResponseBody(toLogBody(response));
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

    private String toLogBody(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return JSON.toJSONString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private boolean shouldRecordSuccess(BizResultWrapper<?> wrapper) {
        return logSuccessEnabled && !isFailure(wrapper);
    }

    private boolean isInventoryListNull(BizResultWrapper<?> wrapper) {
        Object body = wrapper == null ? null : wrapper.getBody();
        if (!(body instanceof SaasSkuStockInventoryBatchQueryResult result)) {
            return false;
        }
        return result.getData() == null || result.getData().getInventory_list() == null;
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
        if (body instanceof SaasSkuStockInventoryBatchQueryResult result) {
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
        if (body instanceof SaasSkuStockInventoryBatchQueryResult result) {
            return result.getError();
        }
        return null;
    }

    @Override
    public BillListRespDTO getBillList(BillListReqDTO req) {
        long startTime = System.currentTimeMillis();
        String traceId = null;
        String merchantCode = req.getMerchantCode();
        String platformStoreId = req.getErpStoreCode();

        EleApiConfig config = eleApiConfigMapper.selectActive();
        if (config == null) {
            log.error("【账单列表API】未找到激活的API配置");
            throw new RuntimeException("未找到激活的API配置");
        }

        try {
            trafficInterceptor.beforeRequest(BILL_LIST_API_CODE, null, req);

            SaasBillListBody body = new SaasBillListBody();
            body.setMerchant_code(req.getMerchantCode());
            body.setErp_store_code(req.getErpStoreCode());
            body.setBill_date(req.getBillDate());
            body.setStatus(req.getStatus() != null ? req.getStatus() : 1);
            if (req.getPageNum() != null) {
                body.setPage_num(req.getPageNum());
            }
            if (req.getPageSize() != null) {
                body.setPage_size(req.getPageSize());
            }

            SaasBillListParam param = new SaasBillListParam();
            param.setTicket(UUID.randomUUID().toString().toUpperCase());
            param.setEncrypt("aes");
            param.setBody(body);

            BizResultWrapper<SaasBillListResult> wrapper = buildBillListExecutor(config).send(param);
            traceId = wrapper == null ? null : wrapper.getTraceid();
            boolean success = !isFailure(wrapper);

            trafficInterceptor.afterResponse(BILL_LIST_API_CODE, traceId, wrapper, success,
                    (int) (System.currentTimeMillis() - startTime));

            if (shouldRecordSuccess(wrapper)) {
                recordBillLog(BILL_LIST_API_CODE, "账单列表", traceId, param, wrapper,
                        merchantCode, platformStoreId, null, null, true, resultCodeForBill(wrapper), resultMsgForBill(wrapper),
                        (int) (System.currentTimeMillis() - startTime));
            } else if (isFailure(wrapper)) {
                recordBillLog(BILL_LIST_API_CODE, "账单列表", traceId, param, wrapper,
                        merchantCode, platformStoreId, null, null, false, resultCodeForBill(wrapper), resultMsgForBill(wrapper),
                        (int) (System.currentTimeMillis() - startTime));
            }

            if (wrapper == null || wrapper.getBody() == null) {
                log.error("【账单列表API】API返回为空");
                throw new RuntimeException("账单列表API返回为空");
            }

            SaasBillListResult result = wrapper.getBody();
            if (!"0".equals(result.getErrno())) {
                log.error("【账单列表API】调用失败，errno={}, error={}", result.getErrno(), result.getError());
                throw new RuntimeException("账单列表API调用失败: " + result.getError());
            }

            if (result.getData() == null) {
                return new BillListRespDTO();
            }

            BillListRespDTO resp = new BillListRespDTO();
            resp.setTotal(result.getData().getTotal());
            resp.setBillDetails(convertToBillDetailDTOList(result.getData().getBillDetails()));

            return resp;
        } catch (Exception e) {
            trafficInterceptor.afterResponse(BILL_LIST_API_CODE, traceId, null, false,
                    (int) (System.currentTimeMillis() - startTime));
            recordBillLog(BILL_LIST_API_CODE, "账单列表", null, req, null,
                    merchantCode, platformStoreId, null, null, false, null, e.getMessage(),
                    (int) (System.currentTimeMillis() - startTime));
            throw new RuntimeException("账单列表API调用异常: " + e.getMessage(), e);
        }
    }

    protected ApiExecutor<SaasBillListResult> buildBillListExecutor(EleApiConfig config) {
        return new ApiExecutor<>(config.getAppId(), config.getAppSecret());
    }

    private String resultCodeForBill(BizResultWrapper<?> wrapper) {
        Object body = wrapper == null ? null : wrapper.getBody();
        if (body instanceof SaasBillListResult result) {
            return result.getErrno();
        }
        return null;
    }

    private String resultMsgForBill(BizResultWrapper<?> wrapper) {
        Object body = wrapper == null ? null : wrapper.getBody();
        if (body instanceof SaasBillListResult result) {
            return result.getError();
        }
        return null;
    }

    private List<BillListRespDTO.BillDetailDTO> convertToBillDetailDTOList(List<SaasBillListResult.BillDetailDTO> billDetails) {
        if (CollUtil.isEmpty(billDetails)) {
            return Collections.emptyList();
        }
        return billDetails.stream().map(src -> {
            BillListRespDTO.BillDetailDTO dst = new BillListRespDTO.BillDetailDTO();
            dst.setCreateTime(src.getCreateTime());
            dst.setUpdateTime(src.getUpdateTime());
            dst.setMerchantCode(src.getMerchantCode());
            dst.setStoreCode(src.getStoreCode());
            dst.setStoreName(src.getStoreName());
            dst.setShopId(src.getShopId());
            dst.setChannelType(src.getChannelType());
            dst.setBillDate(src.getBillDate());
            dst.setOrderDate(src.getOrderDate());
            dst.setOrderId(src.getOrderId());
            dst.setItemPrice(src.getItemPrice());
            dst.setPackageFee(src.getPackageFee());
            dst.setDeliveryFee(src.getDeliveryFee());
            dst.setShopMarketingFee(src.getShopMarketingFee());
            dst.setPlatformFee(src.getPlatformFee());
            dst.setDonationFee(src.getDonationFee());
            dst.setBillAmount(src.getBillAmount());
            dst.setStatus(src.getStatus());
            dst.setBillTypeDesc(src.getBillTypeDesc());
            dst.setRefundId(src.getRefundId());
            dst.setSettleOrderId(src.getSettleOrderId());
            dst.setShippingType(src.getShippingType());
            dst.setUserPayShippingAmount(src.getUserPayShippingAmount());
            dst.setUserOnlinePayAmount(src.getUserOnlinePayAmount());
            dst.setProductPreferences(src.getProductPreferences());
            dst.setNotProductPreferences(src.getNotProductPreferences());
            dst.setPerformanceServiceFee(src.getPerformanceServiceFee());
            dst.setPlatformChargeFee(src.getPlatformChargeFee());
            dst.setActivityAmount(src.getActivityAmount());
            return dst;
        }).collect(Collectors.toList());
    }

    private void recordBillLog(String apiCode, String apiName, String externalTraceId, Object request, Object response,
                               String merchantCode, String platformStoreId, String orderId, String channelOrderId, boolean success,
                               String resultCode, String resultMsg, Integer durationMs) {
        ExternalApiCallLogCreateReqBO reqBO = new ExternalApiCallLogCreateReqBO();
        reqBO.setPlatformCode(PLATFORM_CODE);
        reqBO.setApiCode(apiCode);
        reqBO.setApiName(apiName);
        reqBO.setBizType("BILL");
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
        reqBO.setErpStoreCode(platformStoreId);
        reqBO.setOrderId(orderId);
        reqBO.setChannelOrderId(channelOrderId);
        externalApiCallLogService.createExternalApiCallLog(reqBO);
    }
}
