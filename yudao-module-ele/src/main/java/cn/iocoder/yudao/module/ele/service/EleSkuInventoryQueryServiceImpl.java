package cn.iocoder.yudao.module.ele.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreProductDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreStockDO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SkuTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreProductMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreStockMapper;
import cn.iocoder.yudao.module.business.service.store.StoreService;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleApiConfig;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryGovernancePoolDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryShadowDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleApiConfigMapper;
import cn.iocoder.yudao.module.ele.service.bo.EleSkuInventoryBatchQueryReqBO;
import cn.iocoder.yudao.module.ele.service.bo.EleSkuInventoryShadowUpsertReqBO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreInventoryIngestResultBO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreInventoryIngestRowBO;
import cn.iocoder.yudao.module.ele.service.client.EleOpenApiClient;
import cn.iocoder.yudao.module.ele.service.dto.EleSkuInventoryBatchQueryRespDTO;
import cn.iocoder.yudao.module.ele.service.validator.StoreIdentityValidationResult;
import cn.iocoder.yudao.module.ele.service.validator.StoreIdentityValidator;
import com.alibaba.ocean.rawsdk.common.BizResultWrapper;
import lib.ele.retail.param.ErpSkuInventoryResultDTO;
import lib.ele.retail.param.MeEleRetailSaasSkuStockInventoryBatchQueryReqDto;
import lib.ele.retail.param.MeEleRetailSaasSkuStockInventoryBatchQueryResDto;
import lib.ele.retail.param.OwnerInfo;
import lib.ele.retail.param.SaasSkuStockInventoryBatchQueryParam;
import lib.ele.retail.param.SaasSkuStockInventoryBatchQueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
public class EleSkuInventoryQueryServiceImpl implements EleSkuInventoryQueryService {

    private static final Long DEFAULT_ELE_PLATFORM_ID = 1L;
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_PARTIAL_SUCCESS = "PARTIAL_SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String PERSIST_STATUS_FORMAL = "FORMAL";
    private static final String PERSIST_STATUS_SHADOW = "SHADOW";
    private static final String MATCH_STATUS_SKU_NOT_MATCHED = "SKU_NOT_MATCHED";
    private static final String REASON_CODE_SKU_NOT_FOUND = "SKU_NOT_FOUND";
    private static final String ERROR_WRAPPER_NULL = "UPSTREAM_WRAPPER_NULL";
    private static final String ERROR_BODY_NULL = "UPSTREAM_BODY_NULL";
    private static final String ERROR_DATA_NULL = "UPSTREAM_DATA_NULL";
    private static final String ERROR_INVENTORY_LIST_NULL = "UPSTREAM_INVENTORY_LIST_NULL";
    private static final String ERROR_INVENTORY_ROW_MISSING = "INVENTORY_ROW_MISSING";
    private static final String ERROR_INVENTORY_KEY_MISSING = "INVENTORY_KEY_MISSING";
    private static final String ERROR_INVENTORY_DUPLICATE_ROW = "INVENTORY_DUPLICATE_ROW";

    private final StoreService storeService;
    private final EleApiRateLimiter eleApiRateLimiter;
    private final EleApiConfigMapper eleApiConfigMapper;
    private final EleOpenApiClient eleOpenApiClient;
    private final SkuTableMapper skuTableMapper;
    private final StoreProductMapper storeProductMapper;
    private final StoreStockMapper storeStockMapper;
    private final EleSkuInventoryShadowService shadowService;
    private final EleSkuInventoryGovernanceService governanceService;
    private final EleStoreInventoryIngestService inventoryIngestService;
    private final StoreIdentityValidator storeIdentityValidator;

    @Autowired
    public EleSkuInventoryQueryServiceImpl(StoreService storeService, EleApiRateLimiter eleApiRateLimiter,
                                           EleApiConfigMapper eleApiConfigMapper, EleOpenApiClient eleOpenApiClient,
                                           SkuTableMapper skuTableMapper, StoreProductMapper storeProductMapper,
                                           StoreStockMapper storeStockMapper, EleSkuInventoryShadowService shadowService,
                                           EleSkuInventoryGovernanceService governanceService,
                                           EleStoreInventoryIngestService inventoryIngestService,
                                           StoreIdentityValidator storeIdentityValidator) {
        this.storeService = storeService;
        this.eleApiRateLimiter = eleApiRateLimiter;
        this.eleApiConfigMapper = eleApiConfigMapper;
        this.eleOpenApiClient = eleOpenApiClient;
        this.skuTableMapper = skuTableMapper;
        this.storeProductMapper = storeProductMapper;
        this.storeStockMapper = storeStockMapper;
        this.shadowService = shadowService;
        this.governanceService = governanceService;
        this.inventoryIngestService = inventoryIngestService;
        this.storeIdentityValidator = storeIdentityValidator;
    }

    @Override
    public EleSkuInventoryBatchQueryRespDTO queryBatch(EleSkuInventoryBatchQueryReqBO reqBO) {
        EleSkuInventoryBatchQueryReqBO normalizedReq = normalize(reqBO);
        validateNormalizedReq(normalizedReq);
        eleApiRateLimiter.acquirePermit(EleApiRateLimiter.API_SKU_STOCK_INVENTORY_BATCH_QUERY);
        EleApiConfig config = getApiConfig();
        SaasSkuStockInventoryBatchQueryParam param = buildParam(normalizedReq);
        BizResultWrapper<SaasSkuStockInventoryBatchQueryResult> wrapper = eleOpenApiClient.sendSkuStockInventoryBatchQuery(
                config, param, normalizedReq.getMerchantCode(), normalizedReq.getPlatformStoreId(), normalizedReq.getErpStoreCode());
        return convertResponse(normalizedReq, wrapper);
    }

    private SaasSkuStockInventoryBatchQueryParam buildParam(EleSkuInventoryBatchQueryReqBO normalizedReq) {
        MeEleRetailSaasSkuStockInventoryBatchQueryReqDto body = new MeEleRetailSaasSkuStockInventoryBatchQueryReqDto();
        body.setMerchant_code(normalizedReq.getMerchantCode());
        body.setErp_store_code(normalizedReq.getErpStoreCode());
        if (!normalizedReq.getSkuCodes().isEmpty()) {
            body.setSku_code_list(normalizedReq.getSkuCodes().toArray(String[]::new));
        }

        SaasSkuStockInventoryBatchQueryParam param = new SaasSkuStockInventoryBatchQueryParam();
        param.setTicket(UUID.randomUUID().toString().toUpperCase());
        param.setEncrypt("aes");
        param.setBody(body);
        return param;
    }

    private EleSkuInventoryBatchQueryRespDTO convertResponse(EleSkuInventoryBatchQueryReqBO normalizedReq,
                                                             BizResultWrapper<SaasSkuStockInventoryBatchQueryResult> wrapper) {
        EleSkuInventoryBatchQueryRespDTO respDTO = initResponse(normalizedReq);
        SaasSkuStockInventoryBatchQueryResult result = validateUpstreamSuccess(respDTO, wrapper);
        if (result == null) {
            return respDTO;
        }

        MeEleRetailSaasSkuStockInventoryBatchQueryResDto data = result.getData();
        if (data == null) {
            return buildFailedResponse(respDTO, ERROR_DATA_NULL);
        }

        ErpSkuInventoryResultDTO[] inventoryList = data.getInventory_list();
        if (inventoryList == null) {
            return buildFailedResponse(respDTO, ERROR_INVENTORY_LIST_NULL);
        }

        List<EleSkuInventoryBatchQueryRespDTO.InventoryRowDTO> inventoryRows = new ArrayList<>();
        LinkedHashSet<String> returnedKeys = new LinkedHashSet<>();
        for (ErpSkuInventoryResultDTO inventory : inventoryList) {
            if (inventory == null) {
                continue;
            }
            EleSkuInventoryBatchQueryRespDTO.InventoryRowDTO row = convertInventoryRow(inventory);
            inventoryRows.add(row);
            if (isInventoryKeyMissing(row)) {
                addFailureCount(respDTO);
                addIntegrityError(respDTO, ERROR_INVENTORY_KEY_MISSING);
                continue;
            }
            LinkedHashSet<String> rowKeys = new LinkedHashSet<>(collectResponseKeys(row));
            boolean duplicateRow = false;
            for (String responseKey : rowKeys) {
                if (returnedKeys.contains(responseKey)) {
                    addIntegrityError(respDTO, ERROR_INVENTORY_DUPLICATE_ROW + ":" + responseKey);
                    duplicateRow = true;
                }
            }
            if (duplicateRow) {
                addFailureCount(respDTO);
                continue;
            }
            returnedKeys.addAll(rowKeys);
            processInventoryRow(normalizedReq, inventory, row, respDTO);
        }
        reconcileRequestedKeys(normalizedReq, returnedKeys, respDTO);
        respDTO.setInventoryRows(inventoryRows);
        respDTO.setResponseRowCount(inventoryRows.size());
        finalizeStatus(respDTO);
        return respDTO;
    }

    private EleSkuInventoryBatchQueryRespDTO.InventoryRowDTO convertInventoryRow(ErpSkuInventoryResultDTO inventory) {
        EleSkuInventoryBatchQueryRespDTO.InventoryRowDTO row = new EleSkuInventoryBatchQueryRespDTO.InventoryRowDTO();
        row.setSkuCode(inventory.getSku_code());
        row.setSubSkuCode(inventory.getSub_sku_code());
        row.setAvailableForSale(inventory.getAvailable_for_sale());
        row.setReservedAmount(inventory.getReserved_amount());
        row.setPhysicalStockTotalAmount(inventory.getPhysical_stock_total_amount());
        row.setPhysicalStockAvailableAmount(inventory.getPhysical_stock_available_amount());
        row.setPhysicalStockOccupiedAmount(inventory.getPhysical_stock_occupied_amount());
        row.setPhysicalStockIntransitAmount(inventory.getPhysical_stock_intransit_amount());

        OwnerInfo ownerInfo = inventory.getOwner_info();
        if (ownerInfo != null) {
            row.setOwnerCode(ownerInfo.getOwner_code());
            row.setOwnerName(ownerInfo.getOwner_name());
        }
        return row;
    }

    private void processInventoryRow(EleSkuInventoryBatchQueryReqBO normalizedReq,
                                     ErpSkuInventoryResultDTO inventory,
                                     EleSkuInventoryBatchQueryRespDTO.InventoryRowDTO row,
                                     EleSkuInventoryBatchQueryRespDTO respDTO) {
        String skuCode = normalizeNullable(row.getSkuCode());
        String subSkuCode = normalizeNullable(row.getSubSkuCode());
        if (StrUtil.isBlank(skuCode) && StrUtil.isBlank(subSkuCode)) {
            return;
        }
        EleStoreInventoryIngestResultBO ingestResult = inventoryIngestService.ingest(buildIngestRow(normalizedReq, inventory, row));
        if (PERSIST_STATUS_FORMAL.equals(ingestResult.getPersistStatus())) {
            respDTO.setFormalSuccessCount(respDTO.getFormalSuccessCount() + 1);
        }
        if (PERSIST_STATUS_SHADOW.equals(ingestResult.getPersistStatus())) {
            respDTO.setShadowSuccessCount(respDTO.getShadowSuccessCount() + 1);
        }
        if (ingestResult.getGovernanceId() != null) {
            respDTO.setGovernanceCount(respDTO.getGovernanceCount() + 1);
        }
        row.setPersistStatus(ingestResult.getPersistStatus());
        row.setReasonCode(ingestResult.getReasonCode());
    }

    private EleStoreInventoryIngestRowBO buildIngestRow(EleSkuInventoryBatchQueryReqBO normalizedReq,
                                                         ErpSkuInventoryResultDTO inventory,
                                                         EleSkuInventoryBatchQueryRespDTO.InventoryRowDTO row) {
        EleStoreInventoryIngestRowBO ingestRow = new EleStoreInventoryIngestRowBO();
        ingestRow.setPlatformId(normalizedReq.getPlatformId() == null ? DEFAULT_ELE_PLATFORM_ID : normalizedReq.getPlatformId());
        ingestRow.setMerchantCode(normalizedReq.getMerchantCode());
        ingestRow.setErpStoreCode(normalizedReq.getErpStoreCode());
        ingestRow.setPlatformStoreId(normalizedReq.getPlatformStoreId());
        ingestRow.setStoreId(normalizedReq.getStoreId());
        ingestRow.setSkuCode(normalizeNullable(row.getSkuCode()));
        ingestRow.setSubSkuCode(normalizeNullable(row.getSubSkuCode()));
        ingestRow.setAvailableForSale(row.getAvailableForSale());
        ingestRow.setReservedAmount(row.getReservedAmount());
        ingestRow.setPhysicalStockTotalAmount(row.getPhysicalStockTotalAmount());
        ingestRow.setPhysicalStockAvailableAmount(row.getPhysicalStockAvailableAmount());
        ingestRow.setPhysicalStockOccupiedAmount(row.getPhysicalStockOccupiedAmount());
        ingestRow.setPhysicalStockIntransitAmount(row.getPhysicalStockIntransitAmount());
        ingestRow.setOwnerCode(row.getOwnerCode());
        ingestRow.setOwnerName(row.getOwnerName());
        ingestRow.setRawPayload(JSONUtil.toJsonStr(inventory));
        return ingestRow;
    }

    private boolean upsertFormalStock(EleSkuInventoryBatchQueryReqBO normalizedReq, SkuTableDO sku,
                                      EleSkuInventoryBatchQueryRespDTO.InventoryRowDTO row) {
        StoreProductDO storeProduct = storeProductMapper.selectByStoreIdAndProductSkuId(
                normalizedReq.getStoreId(), String.valueOf(sku.getProductSkuId()));
        if (storeProduct == null) {
            return false;
        }
        StoreStockDO storeStock = storeStockMapper.selectByStoreProductId(storeProduct.getStoreProductId());
        if (storeStock == null) {
            storeStock = new StoreStockDO();
            storeStock.setStoreStockId(IdUtil.fastSimpleUUID());
            storeStock.setStoreProductId(storeProduct.getStoreProductId());
            storeStock.setStoreStockOutstockHours(0);
            fillFormalStock(row, storeStock);
            return storeStockMapper.insert(storeStock) > 0;
        }
        fillFormalStock(row, storeStock);
        return storeStockMapper.updateById(storeStock) > 0;
    }

    private void fillFormalStock(EleSkuInventoryBatchQueryRespDTO.InventoryRowDTO row, StoreStockDO storeStock) {
        storeStock.setStoreStockQuantity(row.getPhysicalStockTotalAmount());
        storeStock.setStoreStockAvailableQuantity(row.getAvailableForSale());
        storeStock.setStoreStockTransitQuantity(row.getPhysicalStockIntransitAmount());
        storeStock.setStoreStockFrozenQuantity(row.getReservedAmount());
    }

    private EleSkuInventoryShadowUpsertReqBO buildShadowReq(EleSkuInventoryBatchQueryReqBO normalizedReq,
                                                            ErpSkuInventoryResultDTO inventory,
                                                            EleSkuInventoryBatchQueryRespDTO.InventoryRowDTO row) {
        EleSkuInventoryShadowUpsertReqBO reqBO = new EleSkuInventoryShadowUpsertReqBO();
        reqBO.setPlatformId(normalizedReq.getPlatformId() == null ? DEFAULT_ELE_PLATFORM_ID : normalizedReq.getPlatformId());
        reqBO.setMerchantCode(normalizedReq.getMerchantCode());
        reqBO.setErpStoreCode(normalizedReq.getErpStoreCode());
        reqBO.setPlatformStoreId(normalizedReq.getPlatformStoreId());
        reqBO.setStoreId(normalizedReq.getStoreId());
        reqBO.setSkuCode(normalizeNullable(row.getSkuCode()));
        reqBO.setSubSkuCode(normalizeNullable(row.getSubSkuCode()));
        reqBO.setAvailableForSale(row.getAvailableForSale());
        reqBO.setReservedAmount(row.getReservedAmount());
        reqBO.setPhysicalStockTotalAmount(row.getPhysicalStockTotalAmount());
        reqBO.setPhysicalStockAvailableAmount(row.getPhysicalStockAvailableAmount());
        reqBO.setPhysicalStockOccupiedAmount(row.getPhysicalStockOccupiedAmount());
        reqBO.setPhysicalStockIntransitAmount(row.getPhysicalStockIntransitAmount());
        reqBO.setOwnerCode(row.getOwnerCode());
        reqBO.setOwnerName(row.getOwnerName());
        reqBO.setRawPayload(JSONUtil.toJsonStr(inventory));
        return reqBO;
    }

    private EleStoreInventoryGovernancePoolDO buildGovernancePool(EleSkuInventoryShadowUpsertReqBO shadowReq,
                                                                  EleStoreInventoryShadowDO shadow) {
        EleStoreInventoryGovernancePoolDO governancePool = new EleStoreInventoryGovernancePoolDO();
        governancePool.setPlatformId(shadowReq.getPlatformId());
        governancePool.setMerchantCode(shadowReq.getMerchantCode());
        governancePool.setErpStoreCode(shadowReq.getErpStoreCode());
        governancePool.setPlatformStoreId(shadowReq.getPlatformStoreId());
        governancePool.setStoreId(shadowReq.getStoreId());
        governancePool.setSkuCode(shadowReq.getSkuCode());
        governancePool.setSubSkuCode(shadowReq.getSubSkuCode());
        governancePool.setInventoryShadowId(shadow == null ? null : shadow.getId());
        governancePool.setReasonCode(REASON_CODE_SKU_NOT_FOUND);
        governancePool.setReasonMsg(EleSkuInventoryShadowServiceImpl.resolveReasonMsg(REASON_CODE_SKU_NOT_FOUND));
        governancePool.setProcessStatus(EleSkuInventoryGovernanceServiceImpl.STATUS_PENDING);
        governancePool.setRawPayload(shadowReq.getRawPayload());
        governancePool.setRemark(EleSkuInventoryShadowServiceImpl.resolveReasonMsg(REASON_CODE_SKU_NOT_FOUND));
        return governancePool;
    }

    private EleSkuInventoryBatchQueryRespDTO initResponse(EleSkuInventoryBatchQueryReqBO normalizedReq) {
        EleSkuInventoryBatchQueryRespDTO respDTO = new EleSkuInventoryBatchQueryRespDTO();
        respDTO.setPlatformStoreId(normalizedReq.getPlatformStoreId());
        respDTO.setMerchantCode(normalizedReq.getMerchantCode());
        respDTO.setErpStoreCode(normalizedReq.getErpStoreCode());
        respDTO.setRequestSkuCount(countRequestedSkuKeys(normalizedReq));
        return respDTO;
    }

    private Integer countRequestedSkuKeys(EleSkuInventoryBatchQueryReqBO normalizedReq) {
        return normalizedReq.getSkuCodes().size();
    }

    private SaasSkuStockInventoryBatchQueryResult validateUpstreamSuccess(EleSkuInventoryBatchQueryRespDTO respDTO,
                                                                           BizResultWrapper<SaasSkuStockInventoryBatchQueryResult> wrapper) {
        if (wrapper == null) {
            buildFailedResponse(respDTO, ERROR_WRAPPER_NULL);
            return null;
        }

        SaasSkuStockInventoryBatchQueryResult result = wrapper.getBody();
        if (result == null) {
            buildFailedResponse(respDTO, ERROR_BODY_NULL);
            return null;
        }

        String errno = normalizeNullable(result.getErrno());
        if (StrUtil.isNotBlank(errno) && !"0".equals(errno)) {
            buildFailedResponse(respDTO, buildErrnoDetail(errno, result.getError()));
            return null;
        }
        return result;
    }

    private String buildErrnoDetail(String errno, String error) {
        String errorMsg = normalizeNullable(error);
        if (StrUtil.isBlank(errorMsg)) {
            return "UPSTREAM_ERRNO:" + errno;
        }
        return "UPSTREAM_ERRNO:" + errno + ":" + errorMsg;
    }

    private void reconcileRequestedKeys(EleSkuInventoryBatchQueryReqBO normalizedReq, LinkedHashSet<String> returnedKeys,
                                        EleSkuInventoryBatchQueryRespDTO respDTO) {
        LinkedHashSet<String> requestedKeys = collectRequestedKeys(normalizedReq);
        for (String requestedKey : requestedKeys) {
            if (!returnedKeys.contains(requestedKey)) {
                addMissingRowError(respDTO, ERROR_INVENTORY_ROW_MISSING + ":" + requestedKey);
            }
        }
    }

    private LinkedHashSet<String> collectRequestedKeys(EleSkuInventoryBatchQueryReqBO normalizedReq) {
        LinkedHashSet<String> requestedKeys = new LinkedHashSet<>();
        for (String skuCode : normalizedReq.getSkuCodes()) {
            requestedKeys.add(buildSkuKey(skuCode));
        }
        return requestedKeys;
    }

    private List<String> collectResponseKeys(EleSkuInventoryBatchQueryRespDTO.InventoryRowDTO row) {
        List<String> responseKeys = new ArrayList<>();
        String skuCode = normalizeNullable(row.getSkuCode());
        String subSkuCode = normalizeNullable(row.getSubSkuCode());
        if (StrUtil.isNotBlank(skuCode)) {
            responseKeys.add(buildSkuKey(skuCode));
        }
        if (StrUtil.isNotBlank(subSkuCode)) {
            responseKeys.add(buildSubSkuKey(subSkuCode));
        }
        return responseKeys;
    }

    private boolean isInventoryKeyMissing(EleSkuInventoryBatchQueryRespDTO.InventoryRowDTO row) {
        return StrUtil.isBlank(normalizeNullable(row.getSkuCode()))
                && StrUtil.isBlank(normalizeNullable(row.getSubSkuCode()));
    }

    private void addMissingRowError(EleSkuInventoryBatchQueryRespDTO respDTO, String errorDetail) {
        respDTO.setMissingRowCount(respDTO.getMissingRowCount() + 1);
        addFailureCount(respDTO);
        addIntegrityError(respDTO, errorDetail);
    }

    private void addFailureCount(EleSkuInventoryBatchQueryRespDTO respDTO) {
        respDTO.setFailureCount(respDTO.getFailureCount() + 1);
    }

    private void addIntegrityError(EleSkuInventoryBatchQueryRespDTO respDTO, String errorDetail) {
        respDTO.getErrorDetails().add(errorDetail);
    }

    private void finalizeStatus(EleSkuInventoryBatchQueryRespDTO respDTO) {
        if (STATUS_FAILED.equals(respDTO.getStatus())) {
            return;
        }
        if (respDTO.getErrorDetails().stream().anyMatch(error -> StrUtil.startWith(error, "INVENTORY_"))) {
            respDTO.setStatus(STATUS_PARTIAL_SUCCESS);
            return;
        }
        respDTO.setStatus(STATUS_SUCCESS);
    }

    private String buildSkuKey(String skuCode) {
        return "SKU:" + skuCode;
    }

    private String buildSubSkuKey(String subSkuCode) {
        return "SUB:" + subSkuCode;
    }

    private EleSkuInventoryBatchQueryRespDTO buildFailedResponse(EleSkuInventoryBatchQueryRespDTO respDTO, String errorDetail) {
        respDTO.setStatus(STATUS_FAILED);
        respDTO.setFailureCount(respDTO.getFailureCount() + 1);
        respDTO.getErrorDetails().add(errorDetail);
        return respDTO;
    }

    private EleApiConfig getApiConfig() {
        EleApiConfig config = eleApiConfigMapper.selectActive();
        if (config == null) {
            throw new RuntimeException("未找到可用的饿了么API配置");
        }
        return config;
    }

    private EleSkuInventoryBatchQueryReqBO normalize(EleSkuInventoryBatchQueryReqBO reqBO) {
        EleSkuInventoryBatchQueryReqBO normalizedReq = new EleSkuInventoryBatchQueryReqBO();
        String platformStoreId = normalizeNullable(reqBO == null ? null : reqBO.getPlatformStoreId());
        String merchantCode = normalizeNullable(reqBO == null ? null : reqBO.getMerchantCode());
        String erpStoreCode = normalizeNullable(reqBO == null ? null : reqBO.getErpStoreCode());
        String storeId = normalizeNullable(reqBO == null ? null : reqBO.getStoreId());

        StorePlatformRespVO storePlatform = StrUtil.isBlank(platformStoreId)
                ? null
                : storeService.getPlatformTableByPlatformStoreId(platformStoreId);
        StoreIdentityValidationResult identityValidation = storeIdentityValidator.validate(
                platformStoreId,
                merchantCode,
                erpStoreCode,
                storeId,
                storePlatform == null ? null : new StoreIdentityValidator.StoreIdentityInput(
                        storePlatform.getStoreId(), storePlatform.getPlatformStoreId(), storePlatform.getSettlementAccount()),
                null,
                null);
        if (identityValidation.getDecision() == StoreIdentityValidationResult.Decision.REJECT) {
            throw new IllegalArgumentException("门店标识冲突，拒绝执行库存同步");
        }

        normalizedReq.setPlatformStoreId(identityValidation.getPlatformStoreId());
        normalizedReq.setPlatformId(storePlatform == null ? null : storePlatform.getPlatformId());
        normalizedReq.setStoreId(identityValidation.getStoreId());
        normalizedReq.setMerchantCode(identityValidation.getMerchantCode());
        normalizedReq.setErpStoreCode(identityValidation.getErpStoreCode());
        normalizedReq.setSkuCodes(normalizeCodes(reqBO == null ? null : reqBO.getSkuCodes()));
        normalizedReq.setSubSkuCodes(new ArrayList<>());
        return normalizedReq;
    }

    private void validateNormalizedReq(EleSkuInventoryBatchQueryReqBO normalizedReq) {
        if (StrUtil.isBlank(normalizedReq.getMerchantCode())) {
            throw new IllegalArgumentException("merchantCode 不能为空");
        }
        if (StrUtil.isBlank(normalizedReq.getErpStoreCode())) {
            throw new IllegalArgumentException("erpStoreCode 不能为空");
        }
        if (normalizedReq.getSkuCodes().isEmpty()) {
            throw new IllegalArgumentException("skuCodes 不能为空");
        }
    }

    private List<String> normalizeCodes(List<String> codes) {
        if (codes == null) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String code : codes) {
            String trimmedCode = StrUtil.trim(code);
            if (StrUtil.isNotBlank(trimmedCode)) {
                normalized.add(trimmedCode);
            }
        }
        return new ArrayList<>(normalized);
    }

    private String normalizeNullable(String value) {
        String trimmedValue = StrUtil.trim(value);
        return StrUtil.isBlank(trimmedValue) ? null : trimmedValue;
    }

}
