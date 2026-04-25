package cn.iocoder.yudao.module.ele.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SkuTableMapper;
import cn.iocoder.yudao.module.business.service.store.StoreProductSyncWriteService;
import cn.iocoder.yudao.module.business.service.store.StoreService;
import cn.iocoder.yudao.module.business.service.store.bo.StoreProductSyncUpsertReqBO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleApiConfig;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsGovernancePoolDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsSyncLogDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleApiConfigMapper;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsPageSyncResult;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsQueryReqBO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsSyncReqBO;
import cn.iocoder.yudao.module.ele.service.client.EleOpenApiClient;
import cn.iocoder.yudao.module.ele.service.dto.EleStoreGoodsQueryRespDTO;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import com.alibaba.ocean.rawsdk.common.BizResultWrapper;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.Resource;
import lib.ele.retail.param.ChannelGoodsSyncReq;
import lib.ele.retail.param.ChannelSkuSyncReq;
import lib.ele.retail.param.MeEleRetailSaasGoodsStoreQueryBatchReqDto;
import lib.ele.retail.param.MeEleRetailSaasGoodsStoreQueryBatchResDto;
import lib.ele.retail.param.SaasGoodsStoreQueryBatchParam;
import lib.ele.retail.param.SaasGoodsStoreQueryBatchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class EleStoreGoodsSyncServiceImpl implements EleStoreGoodsSyncService {

    private static final Long ELE_PLATFORM_ID = 1L;
    private static final String OPERATION_DELETE = "DELETE";
    private static final String OPERATION_UPDATE = "UPDATE";
    private static final String DEFAULT_POS_STATUS_ON_SHELF = "上架";
    private static final String DEFAULT_POS_STATUS_OFF_SHELF = "下架";
    private static final String DEFAULT_OWNERSHIP = "入店";
    private static final String DEFAULT_GOODS_LEVEL = "StoreGoods";
    private static final String TEST_SWITCH_CONFIG_KEY = "ele.store-goods.sync.test-enabled";
    private static final String TEST_MODE_MARK = "[TEST_MODE]";
    private static final String GOVERNANCE_REASON_SKU_NOT_FOUND = "SKU_NOT_FOUND";
    private static final String STORE_GOODS_QUERY_BATCH_API_CODE = "STORE_GOODS_QUERY_BATCH";
    private static final String STORE_GOODS_QUERY_BATCH_API_NAME = "门店商品批量查询";
    private static final RateLimiter STORE_GOODS_QUERY_RATE_LIMITER = RateLimiter.create(100);

    @Resource
    private StoreService storeService;
    @Resource
    private SkuTableMapper skuTableMapper;
    @Resource
    private StoreProductSyncWriteService storeProductSyncWriteService;
    @Resource
    private EleStoreGoodsGovernanceService governanceService;
    @Resource
    private EleStoreGoodsSyncLogService syncLogService;
    @Resource
    private ConfigApi configApi;
    @Resource
    private EleApiConfigMapper eleApiConfigMapper;
    @Resource
    private EleOpenApiClient eleOpenApiClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncStoreGoods(EleStoreGoodsSyncReqBO reqBO) {
        doSyncStoreGoods(reqBO);
    }

    @Override
    public EleStoreGoodsQueryRespDTO queryStoreGoods(EleStoreGoodsQueryReqBO reqBO) {
        EleApiConfig config = getApiConfig();
        String merchantCode = resolveMerchantCode(reqBO.getMerchantCode(), config);
        String erpStoreCode = StrUtil.trim(reqBO.getErpStoreCode());
        if (StrUtil.isBlank(merchantCode)) {
            throw new RuntimeException("merchantCode不能为空");
        }
        if (StrUtil.isBlank(erpStoreCode)) {
            throw new RuntimeException("erpStoreCode不能为空");
        }

        MeEleRetailSaasGoodsStoreQueryBatchReqDto body = new MeEleRetailSaasGoodsStoreQueryBatchReqDto();
        body.setMerchant_code(merchantCode);
        body.setErp_store_code(erpStoreCode);
        body.setPage_no(normalizePageNo(reqBO.getPageNo()));
        body.setPage_size(normalizePageSize(reqBO.getPageSize()));
        if (CollUtil.isNotEmpty(reqBO.getSkuCodeList())) {
            body.setSku_code_list(reqBO.getSkuCodeList().stream()
                    .filter(StrUtil::isNotBlank)
                    .map(StrUtil::trim)
                    .toArray(String[]::new));
        }

        SaasGoodsStoreQueryBatchParam param = new SaasGoodsStoreQueryBatchParam();
        param.setTicket(UUID.randomUUID().toString().toUpperCase());
        param.setEncrypt("aes");
        param.setBody(body);

        STORE_GOODS_QUERY_RATE_LIMITER.acquire();
        BizResultWrapper<SaasGoodsStoreQueryBatchResult> wrapper = eleOpenApiClient.sendStoreGoodsQueryBatch(config,
                param, merchantCode, erpStoreCode, erpStoreCode);
        return convertQueryResult(wrapper, merchantCode, erpStoreCode);
    }

    @Override
    public Integer queryAndSyncStoreGoods(EleStoreGoodsQueryReqBO reqBO, Boolean testMode) {
        return syncStoreGoodsPage(reqBO, testMode).getSyncCount();
    }

    @Override
    public EleStoreGoodsPageSyncResult syncStoreGoodsPage(EleStoreGoodsQueryReqBO reqBO, Boolean testMode) {
        EleStoreGoodsQueryRespDTO queryResp = queryStoreGoods(reqBO);
        EleStoreGoodsPageSyncResult result = new EleStoreGoodsPageSyncResult();
        result.setPageNo(queryResp == null ? normalizePageNo(reqBO.getPageNo()) : queryResp.getPage());
        result.setPageSize(queryResp == null ? normalizePageSize(reqBO.getPageSize()) : queryResp.getPageSize());
        result.setTotal(queryResp == null ? 0 : queryResp.getTotal());
        result.setSyncCount(0);
        result.setSuccessCount(0);
        result.setFailCount(0);
        result.setGovernanceCount(0);
        if (queryResp == null || CollUtil.isEmpty(queryResp.getGoodsList())) {
            return result;
        }

        for (EleStoreGoodsQueryRespDTO.GoodsItem goodsItem : queryResp.getGoodsList()) {
            if (CollUtil.isEmpty(goodsItem.getSkuList())) {
                continue;
            }
            for (EleStoreGoodsQueryRespDTO.SkuItem skuItem : goodsItem.getSkuList()) {
                EleStoreGoodsSyncReqBO syncReqBO = buildSyncReq(reqBO, queryResp, goodsItem, skuItem, testMode);
                boolean success = doSyncStoreGoods(syncReqBO);
                result.setSyncCount(result.getSyncCount() + 1);
                if (success) {
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } else {
                    result.setFailCount(result.getFailCount() + 1);
                    result.setGovernanceCount(result.getGovernanceCount() + 1);
                }
            }
        }
        return result;
    }

    private EleStoreGoodsSyncReqBO buildSyncReq(EleStoreGoodsQueryReqBO reqBO, EleStoreGoodsQueryRespDTO queryResp,
                                                EleStoreGoodsQueryRespDTO.GoodsItem goodsItem,
                                                EleStoreGoodsQueryRespDTO.SkuItem skuItem, Boolean testMode) {
        EleStoreGoodsSyncReqBO syncReqBO = new EleStoreGoodsSyncReqBO();
        syncReqBO.setApiCode(STORE_GOODS_QUERY_BATCH_API_CODE);
        syncReqBO.setApiName(STORE_GOODS_QUERY_BATCH_API_NAME);
        syncReqBO.setMerchantCode(StrUtil.blankToDefault(goodsItem.getMerchantCode(), queryResp.getMerchantCode()));
        syncReqBO.setErpStoreCode(StrUtil.blankToDefault(goodsItem.getStoreCode(), queryResp.getStoreCode()));
        syncReqBO.setPlatformStoreId(syncReqBO.getErpStoreCode());
        syncReqBO.setSpuCode(goodsItem.getSpuCode());
        syncReqBO.setSkuCode(skuItem.getSkuCode());
        syncReqBO.setSubSkuCode(skuItem.getSubSkuCode());
        syncReqBO.setGoodsLevel(DEFAULT_GOODS_LEVEL);
        syncReqBO.setOperationType(resolveOperationType(skuItem.getStatus()));
        syncReqBO.setStoreProductIsActive(resolveStoreProductIsActive(skuItem.getStatus()));
        syncReqBO.setStoreProductPosStatus(resolveStoreProductPosStatus(skuItem.getStatus()));
        syncReqBO.setStoreProductPrice(convertSalePrice(skuItem.getSalePrice()));
        syncReqBO.setPageNo(queryResp.getPage());
        syncReqBO.setPageSize(queryResp.getPageSize());
        syncReqBO.setDataCount(queryResp.getTotal());
        syncReqBO.setRequestBody(String.valueOf(reqBO));
        syncReqBO.setResponseBody(String.valueOf(goodsItem));
        syncReqBO.setRawPayload(String.valueOf(skuItem));
        syncReqBO.setTestMode(testMode);
        return syncReqBO;
    }

    private boolean doSyncStoreGoods(EleStoreGoodsSyncReqBO reqBO) {
        String merchantCode = StrUtil.trim(reqBO.getMerchantCode());
        String erpStoreCode = resolveErpStoreCode(reqBO);
        String skuCode = StrUtil.trim(reqBO.getSkuCode());
        boolean testMode = Boolean.TRUE.equals(reqBO.getTestMode());

        if (StrUtil.isBlank(merchantCode)) {
            throw new RuntimeException("merchantCode不能为空");
        }
        if (StrUtil.isBlank(erpStoreCode)) {
            throw new RuntimeException("erpStoreCode不能为空");
        }
        if (StrUtil.isBlank(skuCode)) {
            throw new RuntimeException("skuCode不能为空");
        }
        validateTestMode(testMode);

        List<StorePlatformRespVO> stores = storeService.getPlatformTableListByPlatformStoreId(ELE_PLATFORM_ID, erpStoreCode);
        if (CollUtil.isEmpty(stores)) {
            writeFailureLog(reqBO, null, "STORE_NOT_FOUND", appendTestMode(testMode, "未找到门店映射: " + erpStoreCode));
            throw new RuntimeException("未找到门店映射: " + erpStoreCode);
        }

        SkuTableDO sku = skuTableMapper.selectByProductSkuCode(skuCode);
        if (sku == null) {
            for (StorePlatformRespVO store : stores) {
                createGovernanceRecord(reqBO, store, testMode);
                writeFailureLog(reqBO, store, GOVERNANCE_REASON_SKU_NOT_FOUND,
                        appendTestMode(testMode, "skuCode未匹配本地SKU: " + skuCode));
            }
            return false;
        }

        for (StorePlatformRespVO store : stores) {
            upsertStoreProduct(reqBO, store, sku, testMode);
            writeSuccessLog(reqBO, store, testMode);
        }
        return true;
    }

    private EleStoreGoodsQueryRespDTO convertQueryResult(BizResultWrapper<SaasGoodsStoreQueryBatchResult> wrapper,
                                                         String merchantCode, String erpStoreCode) {
        EleStoreGoodsQueryRespDTO respDTO = new EleStoreGoodsQueryRespDTO();
        respDTO.setMerchantCode(merchantCode);
        respDTO.setStoreCode(erpStoreCode);
        if (wrapper == null || wrapper.getBody() == null) {
            return respDTO;
        }

        SaasGoodsStoreQueryBatchResult result = wrapper.getBody();
        if (!"0".equals(result.getErrno()) || result.getData() == null) {
            throw new RuntimeException(StrUtil.blankToDefault(result.getError(), "商品批量查询失败"));
        }

        MeEleRetailSaasGoodsStoreQueryBatchResDto data = result.getData();
        respDTO.setMerchantCode(StrUtil.blankToDefault(data.getMerchant_code(), merchantCode));
        respDTO.setStoreCode(StrUtil.blankToDefault(data.getStore_code(), erpStoreCode));
        respDTO.setPage(data.getPage());
        respDTO.setTotal(data.getTotal());
        respDTO.setPageSize(data.getPage_size());
        if (data.getChannelGoodsSyncReqList() == null) {
            return respDTO;
        }

        List<EleStoreGoodsQueryRespDTO.GoodsItem> goodsList = new ArrayList<>();
        for (ChannelGoodsSyncReq source : data.getChannelGoodsSyncReqList()) {
            EleStoreGoodsQueryRespDTO.GoodsItem goodsItem = new EleStoreGoodsQueryRespDTO.GoodsItem();
            goodsItem.setMerchantCode(source.getMerchant_code());
            goodsItem.setStoreCode(source.getStore_code());
            goodsItem.setTitle(source.getTitle());
            goodsItem.setSpuCode(source.getSpu_code());
            goodsItem.setMainPic(source.getMain_pic());
            goodsItem.setSubPics(source.getSub_pics());
            if (source.getSku_list() != null) {
                List<EleStoreGoodsQueryRespDTO.SkuItem> skuList = new ArrayList<>();
                for (ChannelSkuSyncReq skuSource : source.getSku_list()) {
                    EleStoreGoodsQueryRespDTO.SkuItem skuItem = new EleStoreGoodsQueryRespDTO.SkuItem();
                    skuItem.setSkuCode(skuSource.getSku_code());
                    skuItem.setSubSkuCode(skuSource.getSub_sku_code());
                    skuItem.setSpecification(skuSource.getSpecification());
                    skuItem.setSalePrice(skuSource.getSale_price());
                    skuItem.setStatus(source.getStatus());
                    skuList.add(skuItem);
                }
                goodsItem.setSkuList(skuList);
            }
            goodsList.add(goodsItem);
        }
        respDTO.setGoodsList(goodsList);
        return respDTO;
    }

    private void upsertStoreProduct(EleStoreGoodsSyncReqBO reqBO, StorePlatformRespVO store, SkuTableDO sku, boolean testMode) {
        StoreProductSyncUpsertReqBO upsertReqBO = new StoreProductSyncUpsertReqBO();
        upsertReqBO.setStoreId(store.getStoreId());
        upsertReqBO.setProductSkuId(String.valueOf(sku.getProductSkuId()));
        upsertReqBO.setStoreProductOwnership(DEFAULT_OWNERSHIP);
        upsertReqBO.setStoreProductPrice(reqBO.getStoreProductPrice());
        upsertReqBO.setStoreProductFirstDate(LocalDate.now());
        upsertReqBO.setStoreProductShelfTime(LocalDateTime.now());
        if (OPERATION_DELETE.equalsIgnoreCase(StrUtil.blankToDefault(reqBO.getOperationType(), ""))) {
            upsertReqBO.setStoreProductIsActive(0);
            upsertReqBO.setStoreProductPosStatus(DEFAULT_POS_STATUS_OFF_SHELF);
        } else {
            upsertReqBO.setStoreProductIsActive(reqBO.getStoreProductIsActive() == null ? 1 : reqBO.getStoreProductIsActive());
            upsertReqBO.setStoreProductPosStatus(reqBO.getStoreProductPosStatus());
        }
        storeProductSyncWriteService.upsertStoreProduct(upsertReqBO);
        log.info("[Ele商品同步] storeId={}, platformStoreId={}, skuCode={}, operationType={}, testMode={}",
                store.getStoreId(), store.getPlatformStoreId(), reqBO.getSkuCode(), reqBO.getOperationType(), testMode);
    }

    private void createGovernanceRecord(EleStoreGoodsSyncReqBO reqBO, StorePlatformRespVO store, boolean testMode) {
        EleStoreGoodsGovernancePoolDO governancePool = new EleStoreGoodsGovernancePoolDO();
        governancePool.setMerchantCode(StrUtil.trim(reqBO.getMerchantCode()));
        governancePool.setErpStoreCode(resolveErpStoreCode(reqBO));
        governancePool.setPlatformId(ELE_PLATFORM_ID);
        governancePool.setStoreId(store == null ? null : store.getStoreId());
        governancePool.setPlatformStoreId(store == null ? StrUtil.trim(reqBO.getPlatformStoreId()) : store.getPlatformStoreId());
        governancePool.setSkuCode(StrUtil.trim(reqBO.getSkuCode()));
        governancePool.setSubSkuCode(StrUtil.trim(reqBO.getSubSkuCode()));
        governancePool.setSpuCode(StrUtil.trim(reqBO.getSpuCode()));
        governancePool.setGoodsLevel(StrUtil.trim(reqBO.getGoodsLevel()));
        governancePool.setOperationType(StrUtil.trim(reqBO.getOperationType()));
        governancePool.setReasonCode(GOVERNANCE_REASON_SKU_NOT_FOUND);
        governancePool.setReasonMsg("skuCode未匹配本地SKU");
        governancePool.setProcessStatus("PENDING");
        governancePool.setRawPayload(StrUtil.blankToDefault(reqBO.getRawPayload(), reqBO.getRequestBody()));
        governancePool.setRemark(testMode ? TEST_MODE_MARK : null);
        governanceService.create(governancePool);
    }

    private void writeSuccessLog(EleStoreGoodsSyncReqBO reqBO, StorePlatformRespVO store, boolean testMode) {
        EleStoreGoodsSyncLogDO syncLog = buildSyncLog(reqBO, store);
        syncLog.setSuccess(Boolean.TRUE);
        syncLog.setResultCode("SUCCESS");
        syncLog.setResultMsg(appendTestMode(testMode, "处理成功"));
        syncLogService.create(syncLog);
    }

    private void writeFailureLog(EleStoreGoodsSyncReqBO reqBO, StorePlatformRespVO store, String resultCode, String resultMsg) {
        EleStoreGoodsSyncLogDO syncLog = buildSyncLog(reqBO, store);
        syncLog.setSuccess(Boolean.FALSE);
        syncLog.setResultCode(resultCode);
        syncLog.setResultMsg(resultMsg);
        syncLogService.create(syncLog);
    }

    private EleStoreGoodsSyncLogDO buildSyncLog(EleStoreGoodsSyncReqBO reqBO, StorePlatformRespVO store) {
        EleStoreGoodsSyncLogDO syncLog = new EleStoreGoodsSyncLogDO();
        syncLog.setTraceId(StrUtil.trim(reqBO.getTraceId()));
        syncLog.setTicket(StrUtil.trim(reqBO.getTicket()));
        syncLog.setApiCode(StrUtil.trim(reqBO.getApiCode()));
        syncLog.setApiName(StrUtil.trim(reqBO.getApiName()));
        syncLog.setMerchantCode(StrUtil.trim(reqBO.getMerchantCode()));
        syncLog.setErpStoreCode(resolveErpStoreCode(reqBO));
        syncLog.setPlatformId(ELE_PLATFORM_ID);
        syncLog.setStoreId(store == null ? null : store.getStoreId());
        syncLog.setPlatformStoreId(store == null ? StrUtil.trim(reqBO.getPlatformStoreId()) : store.getPlatformStoreId());
        syncLog.setSkuCode(StrUtil.trim(reqBO.getSkuCode()));
        syncLog.setSubSkuCode(StrUtil.trim(reqBO.getSubSkuCode()));
        syncLog.setOperationType(StrUtil.trim(reqBO.getOperationType()));
        syncLog.setPageNo(reqBO.getPageNo());
        syncLog.setPageSize(reqBO.getPageSize());
        syncLog.setDataCount(reqBO.getDataCount());
        syncLog.setRequestBody(reqBO.getRequestBody());
        syncLog.setResponseBody(reqBO.getResponseBody());
        return syncLog;
    }

    private void validateTestMode(boolean testMode) {
        if (!testMode) {
            return;
        }
        String enabled = StrUtil.trim(configApi.getConfigValueByKey(TEST_SWITCH_CONFIG_KEY));
        if (!"true".equalsIgnoreCase(enabled)) {
            throw new RuntimeException("测试模式未开启");
        }
    }

    private EleApiConfig getApiConfig() {
        EleApiConfig config = eleApiConfigMapper.selectActive();
        if (config == null) {
            throw new RuntimeException("未找到可用的饿了么API配置");
        }
        return config;
    }

    private String resolveMerchantCode(String merchantCode, EleApiConfig config) {
        String resolved = StrUtil.trim(merchantCode);
        if (StrUtil.isBlank(resolved) && config != null) {
            resolved = StrUtil.trim(config.getMerchantCode());
        }
        return resolved;
    }

    private Integer normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    private Integer normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 20);
    }

    private String resolveErpStoreCode(EleStoreGoodsSyncReqBO reqBO) {
        String erpStoreCode = StrUtil.trim(reqBO.getErpStoreCode());
        if (StrUtil.isNotBlank(erpStoreCode)) {
            return erpStoreCode;
        }
        return StrUtil.trim(reqBO.getPlatformStoreId());
    }

    private String resolveOperationType(Integer status) {
        return status != null && status == 3 ? OPERATION_DELETE : OPERATION_UPDATE;
    }

    private Integer resolveStoreProductIsActive(Integer status) {
        return status != null && status == 3 ? 0 : 1;
    }

    private String resolveStoreProductPosStatus(Integer status) {
        return status != null && status == 3 ? DEFAULT_POS_STATUS_OFF_SHELF : DEFAULT_POS_STATUS_ON_SHELF;
    }

    private BigDecimal convertSalePrice(Long salePrice) {
        return salePrice == null ? null : BigDecimal.valueOf(salePrice).movePointLeft(2);
    }

    private String appendTestMode(boolean testMode, String message) {
        return testMode ? TEST_MODE_MARK + " " + message : message;
    }
}
