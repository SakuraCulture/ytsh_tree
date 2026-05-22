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
import cn.iocoder.yudao.module.ele.enums.EleStoreGoodsShadowStatus;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsPageSyncResult;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsQueryReqBO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsShadowUpsertReqBO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsSyncReqBO;
import cn.iocoder.yudao.module.ele.service.client.EleOpenApiClient;
import cn.iocoder.yudao.module.ele.service.dto.EleStoreGoodsQueryRespDTO;
import cn.iocoder.yudao.module.ele.service.validator.StoreIdentityValidationResult;
import cn.iocoder.yudao.module.ele.service.validator.StoreIdentityValidator;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
    @Resource
    private EleStoreGoodsShadowService shadowService;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private StoreIdentityValidator storeIdentityValidator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncStoreGoods(EleStoreGoodsSyncReqBO reqBO) {
        SyncOutcome outcome = doSyncStoreGoods(reqBO);
        if (!outcome.success()) {
            throw new RuntimeException(StrUtil.blankToDefault(outcome.errorMessage(),
                    "商品同步失败: " + StrUtil.trim(reqBO.getSkuCode())));
        }
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
    public EleStoreGoodsPageSyncResult queryAndSyncStoreGoods(EleStoreGoodsQueryReqBO reqBO, Boolean testMode) {
        return syncStoreGoodsPage(reqBO, testMode);
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
        result.setShadowCount(0);
        if (queryResp == null || CollUtil.isEmpty(queryResp.getGoodsList())) {
            return result;
        }

        for (EleStoreGoodsQueryRespDTO.GoodsItem goodsItem : queryResp.getGoodsList()) {
            if (CollUtil.isEmpty(goodsItem.getSkuList())) {
                continue;
            }
            for (EleStoreGoodsQueryRespDTO.SkuItem skuItem : goodsItem.getSkuList()) {
                EleStoreGoodsSyncReqBO syncReqBO = buildSyncReq(reqBO, queryResp, goodsItem, skuItem, testMode);
                result.setSyncCount(result.getSyncCount() + 1);
                try {
                    SyncOutcome outcome = executeSyncInTransaction(syncReqBO);
                    if (outcome.success()) {
                        result.setSuccessCount(result.getSuccessCount() + 1);
                    } else if (outcome.shadowed()) {
                        result.setShadowCount(result.getShadowCount() + 1);
                        result.setGovernanceCount(result.getGovernanceCount() + 1);
                    } else {
                        result.setFailCount(result.getFailCount() + 1);
                    }
                } catch (RuntimeException ex) {
                    log.warn("[Ele商品同步] 分页同步失败，skuCode={}, err={}", syncReqBO.getSkuCode(), ex.getMessage(), ex);
                    result.setFailCount(result.getFailCount() + 1);
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
        syncReqBO.setMerchantCode(StrUtil.blankToDefault(StrUtil.trim(reqBO.getMerchantCode()), queryResp.getMerchantCode()));
        // erpStoreCode 是平台门店ID，必须来自请求参数，不能用 API 返回的 storeCode (本地门店ID) 作为默认值
        String erpStoreCode = StrUtil.trim(reqBO.getErpStoreCode());
        syncReqBO.setErpStoreCode(erpStoreCode);
        // platformStoreId 与 erpStoreCode 语义相同，都是平台门店ID
        syncReqBO.setPlatformStoreId(erpStoreCode);
        syncReqBO.setStoreId(null);
        syncReqBO.setUpstreamMerchantCode(StrUtil.blankToDefault(goodsItem.getMerchantCode(), queryResp.getMerchantCode()));
        // upstreamStoreCode 是 API 返回的本地门店ID
        syncReqBO.setUpstreamStoreCode(StrUtil.blankToDefault(goodsItem.getStoreCode(), queryResp.getStoreCode()));
        syncReqBO.setSpuCode(goodsItem.getSpuCode());
        syncReqBO.setTitle(goodsItem.getTitle());
        syncReqBO.setMainPic(goodsItem.getMainPic());
        syncReqBO.setSkuCode(skuItem.getSkuCode());
        syncReqBO.setSubSkuCode(skuItem.getSubSkuCode());
        syncReqBO.setSpecification(skuItem.getSpecification());
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

    private SyncOutcome executeSyncInTransaction(EleStoreGoodsSyncReqBO reqBO) {
        return transactionTemplate.execute(status -> doSyncStoreGoods(reqBO));
    }

    private SyncOutcome doSyncStoreGoods(EleStoreGoodsSyncReqBO reqBO) {
        String merchantCode = StrUtil.trim(reqBO.getMerchantCode());
        String erpStoreCode = resolveErpStoreCode(reqBO);
        String platformStoreId = StrUtil.trim(reqBO.getPlatformStoreId());
        String storeId = StrUtil.trim(reqBO.getStoreId());
        String upstreamStoreCode = StrUtil.trim(reqBO.getUpstreamStoreCode());
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

        List<StorePlatformRespVO> stores = resolveStores(platformStoreId, upstreamStoreCode, storeId);
        if (CollUtil.isEmpty(stores)) {
            String missingStoreKey = firstNonBlank(platformStoreId, upstreamStoreCode, storeId);
            String errorMsg = "未找到门店映射: " + missingStoreKey;
            // 写入治理记录，便于后续人工处理
            createGovernanceRecordForStoreNotFound(reqBO, missingStoreKey, testMode);
            writeFailureLog(reqBO, null, "STORE_NOT_FOUND", appendTestMode(testMode, errorMsg));
            throw new RuntimeException(errorMsg);
        }

        StoreIdentityValidationResult identityValidation = validateStoreIdentity(reqBO, stores);
        if (identityValidation.getDecision() == StoreIdentityValidationResult.Decision.REJECT) {
            applyIdentityValidation(reqBO, identityValidation);
            StorePlatformRespVO failedStore = stores.get(0);
            String errorMsg = "门店标识冲突，拒绝写入正式表";
            // 写入治理记录，便于后续人工处理
            createGovernanceRecordForIdentityMismatch(reqBO, failedStore, testMode);
            writeFailureLog(reqBO, failedStore, StoreIdentityValidator.REASON_CODE_STORE_IDENTITY_MISMATCH,
                    appendTestMode(testMode, errorMsg));
            return SyncOutcome.failureResult(errorMsg);
        }
        applyIdentityValidation(reqBO, identityValidation);

        SkuTableDO sku = skuTableMapper.selectByProductSkuCode(skuCode);
        if (sku == null) {
            for (StorePlatformRespVO store : stores) {
                shadowService.upsertFromSync(buildShadowReq(reqBO, store), EleStoreGoodsShadowStatus.UNMATCHED, null, null);
                createGovernanceRecord(reqBO, store, testMode);
                writeFailureLog(reqBO, store, GOVERNANCE_REASON_SKU_NOT_FOUND,
                        appendTestMode(testMode, "skuCode未匹配本地SKU，已写入影子门店品: " + skuCode));
            }
            return SyncOutcome.shadowedResult();
        }

        for (StorePlatformRespVO store : stores) {
            upsertStoreProduct(reqBO, store, sku, testMode);
            writeSuccessLog(reqBO, store, testMode);
        }
        return SyncOutcome.successResult();
    }

    private StoreIdentityValidationResult validateStoreIdentity(EleStoreGoodsSyncReqBO reqBO, List<StorePlatformRespVO> stores) {
        StoreIdentityValidationResult lastResult = null;
        String platformStoreId = StrUtil.trim(reqBO.getPlatformStoreId());

        for (StorePlatformRespVO store : stores) {
            // 一店多开时，只验证与 platformStoreId 匹配的 store
            // platformStoreId 来自请求参数 erpStoreCode，是平台门店ID
            if (StrUtil.isNotBlank(platformStoreId)
                    && !StrUtil.equals(platformStoreId, store.getPlatformStoreId())) {
                continue;
            }
            StoreIdentityValidationResult result = storeIdentityValidator.validate(
                    reqBO.getPlatformStoreId(),
                    reqBO.getMerchantCode(),
                    reqBO.getErpStoreCode(),
                    reqBO.getStoreId(),
                    new StoreIdentityValidator.StoreIdentityInput(store.getStoreId(),
                            store.getPlatformStoreId(), store.getSettlementAccount()),
                    reqBO.getUpstreamStoreCode(),
                    reqBO.getUpstreamMerchantCode());
            if (result.getDecision() == StoreIdentityValidationResult.Decision.REJECT) {
                return result;
            }
            lastResult = result;
        }
        return lastResult == null
                ? StoreIdentityValidationResult.reject(StoreIdentityValidator.REASON_CODE_STORE_IDENTITY_MISMATCH,
                reqBO.getPlatformStoreId(), reqBO.getMerchantCode(), reqBO.getErpStoreCode(), reqBO.getStoreId())
                : lastResult;
    }

    private void applyIdentityValidation(EleStoreGoodsSyncReqBO reqBO, StoreIdentityValidationResult validationResult) {
        if (validationResult == null) {
            return;
        }
        reqBO.setPlatformStoreId(validationResult.getPlatformStoreId());
        reqBO.setMerchantCode(validationResult.getMerchantCode());
        reqBO.setErpStoreCode(validationResult.getErpStoreCode());
        reqBO.setStoreId(validationResult.getStoreId());
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

    private String upsertStoreProduct(EleStoreGoodsSyncReqBO reqBO, StorePlatformRespVO store, SkuTableDO sku, boolean testMode) {
        StoreProductSyncUpsertReqBO upsertReqBO = new StoreProductSyncUpsertReqBO();
        upsertReqBO.setStoreId(store.getStoreId());
        upsertReqBO.setProductSkuId(String.valueOf(sku.getProductSkuId()));
        upsertReqBO.setStoreProductPrice(reqBO.getStoreProductPrice());
        if (OPERATION_DELETE.equalsIgnoreCase(StrUtil.blankToDefault(reqBO.getOperationType(), ""))) {
            upsertReqBO.setStoreProductIsActive(0);
            upsertReqBO.setStoreProductPosStatus(DEFAULT_POS_STATUS_OFF_SHELF);
        } else {
            upsertReqBO.setStoreProductIsActive(reqBO.getStoreProductIsActive() == null ? 1 : reqBO.getStoreProductIsActive());
            upsertReqBO.setStoreProductPosStatus(reqBO.getStoreProductPosStatus());
        }
        String storeProductId = storeProductSyncWriteService.upsertStoreProduct(upsertReqBO);
        shadowService.upsertFromSync(buildShadowReq(reqBO, store), EleStoreGoodsShadowStatus.MERGED,
                String.valueOf(sku.getProductSkuId()), storeProductId);
        log.info("[Ele商品同步] storeId={}, platformStoreId={}, skuCode={}, operationType={}, testMode={}",
                store.getStoreId(), store.getPlatformStoreId(), reqBO.getSkuCode(), reqBO.getOperationType(), testMode);
        return storeProductId;
    }

    private EleStoreGoodsShadowUpsertReqBO buildShadowReq(EleStoreGoodsSyncReqBO reqBO, StorePlatformRespVO store) {
        EleStoreGoodsShadowUpsertReqBO shadowReqBO = new EleStoreGoodsShadowUpsertReqBO();
        shadowReqBO.setPlatformId(ELE_PLATFORM_ID);
        shadowReqBO.setMerchantCode(StrUtil.trim(reqBO.getMerchantCode()));
        shadowReqBO.setErpStoreCode(resolveErpStoreCode(reqBO));
        shadowReqBO.setPlatformStoreId(store == null ? StrUtil.trim(reqBO.getPlatformStoreId()) : store.getPlatformStoreId());
        shadowReqBO.setStoreId(store == null ? null : store.getStoreId());
        shadowReqBO.setSpuCode(StrUtil.trim(reqBO.getSpuCode()));
        shadowReqBO.setSkuCode(StrUtil.trim(reqBO.getSkuCode()));
        shadowReqBO.setSubSkuCode(StrUtil.trim(reqBO.getSubSkuCode()));
        shadowReqBO.setTitle(StrUtil.trim(reqBO.getTitle()));
        shadowReqBO.setMainPic(StrUtil.trim(reqBO.getMainPic()));
        shadowReqBO.setSpecification(StrUtil.trim(reqBO.getSpecification()));
        shadowReqBO.setSalePrice(reqBO.getStoreProductPrice());
        shadowReqBO.setPosStatus(reqBO.getStoreProductPosStatus());
        shadowReqBO.setIsActive(reqBO.getStoreProductIsActive());
        shadowReqBO.setRawPayload(StrUtil.blankToDefault(reqBO.getRawPayload(), reqBO.getResponseBody()));
        return shadowReqBO;
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

    private void createGovernanceRecordForStoreNotFound(EleStoreGoodsSyncReqBO reqBO, String missingStoreKey, boolean testMode) {
        EleStoreGoodsGovernancePoolDO governancePool = new EleStoreGoodsGovernancePoolDO();
        governancePool.setMerchantCode(StrUtil.trim(reqBO.getMerchantCode()));
        governancePool.setErpStoreCode(resolveErpStoreCode(reqBO));
        governancePool.setPlatformId(ELE_PLATFORM_ID);
        governancePool.setPlatformStoreId(StrUtil.trim(reqBO.getPlatformStoreId()));
        governancePool.setSkuCode(StrUtil.trim(reqBO.getSkuCode()));
        governancePool.setSubSkuCode(StrUtil.trim(reqBO.getSubSkuCode()));
        governancePool.setSpuCode(StrUtil.trim(reqBO.getSpuCode()));
        governancePool.setGoodsLevel(StrUtil.trim(reqBO.getGoodsLevel()));
        governancePool.setOperationType(StrUtil.trim(reqBO.getOperationType()));
        governancePool.setReasonCode("STORE_NOT_FOUND");
        governancePool.setReasonMsg("未找到门店映射: " + missingStoreKey);
        governancePool.setProcessStatus("PENDING");
        governancePool.setRawPayload(StrUtil.blankToDefault(reqBO.getRawPayload(), reqBO.getRequestBody()));
        governancePool.setRemark(testMode ? TEST_MODE_MARK : null);
        governanceService.create(governancePool);
    }

    private void createGovernanceRecordForIdentityMismatch(EleStoreGoodsSyncReqBO reqBO, StorePlatformRespVO store, boolean testMode) {
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
        governancePool.setReasonCode(StoreIdentityValidator.REASON_CODE_STORE_IDENTITY_MISMATCH);
        governancePool.setReasonMsg("门店标识冲突，拒绝写入正式表");
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
        // 增强错误消息，包含门店和商品标识，便于追踪
        String enrichedMsg = enrichFailureMessage(reqBO, store, resultMsg);
        syncLog.setResultMsg(enrichedMsg);
        syncLogService.create(syncLog);
    }

    private String enrichFailureMessage(EleStoreGoodsSyncReqBO reqBO, StorePlatformRespVO store, String baseMsg) {
        StringBuilder sb = new StringBuilder();
        sb.append("[门店:");
        if (store != null) {
            sb.append("storeId=").append(store.getStoreId())
              .append(",platformStoreId=").append(store.getPlatformStoreId());
        } else {
            sb.append("erpStoreCode=").append(StrUtil.trim(reqBO.getErpStoreCode()));
        }
        sb.append("] [商品:skuCode=").append(StrUtil.trim(reqBO.getSkuCode())).append("] ");
        sb.append(baseMsg);
        return sb.toString();
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
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

    private List<StorePlatformRespVO> resolveStores(String platformStoreId, String upstreamStoreCode, String storeId) {
        if (StrUtil.isNotBlank(platformStoreId)) {
            List<StorePlatformRespVO> stores = storeService.getPlatformTableListByPlatformStoreId(ELE_PLATFORM_ID, platformStoreId);
            if (CollUtil.isNotEmpty(stores)) {
                return stores;
            }
        }
        if (StrUtil.isNotBlank(upstreamStoreCode)) {
            List<StorePlatformRespVO> stores = storeService.getPlatformTableListByPlatformStoreId(ELE_PLATFORM_ID, upstreamStoreCode);
            if (CollUtil.isNotEmpty(stores)) {
                return stores;
            }
        }
        if (StrUtil.isBlank(storeId)) {
            return Collections.emptyList();
        }
        return storeService.getPlatformTableListByStoreId(storeId).stream()
                .filter(store -> Objects.equals(store.getPlatformId(), ELE_PLATFORM_ID))
                .filter(store -> store.getStatus() != null && store.getStatus() == 1)
                .toList();
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

    private record SyncOutcome(boolean success, boolean shadowed, String errorMessage) {

        static SyncOutcome successResult() {
            return new SyncOutcome(true, false, null);
        }

        static SyncOutcome shadowedResult() {
            return new SyncOutcome(false, true, null);
        }

        static SyncOutcome failureResult(String errorMessage) {
            return new SyncOutcome(false, false, errorMessage);
        }
    }
}
