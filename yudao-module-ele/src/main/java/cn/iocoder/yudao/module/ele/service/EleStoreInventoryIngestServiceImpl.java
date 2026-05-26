package cn.iocoder.yudao.module.ele.service;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreProductDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreStockDO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SkuTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreProductMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreStockMapper;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryGovernancePoolDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryShadowDO;
import cn.iocoder.yudao.module.ele.service.bo.EleSkuInventoryShadowUpsertReqBO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreInventoryIngestResultBO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreInventoryIngestRowBO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EleStoreInventoryIngestServiceImpl implements EleStoreInventoryIngestService {

    private static final Long DEFAULT_ELE_PLATFORM_ID = 1L;

    @Resource
    private SkuTableMapper skuTableMapper;
    @Resource
    private StoreProductMapper storeProductMapper;
    @Resource
    private StoreStockMapper storeStockMapper;
    @Resource
    private EleSkuInventoryShadowService shadowService;
    @Resource
    private EleSkuInventoryGovernanceService governanceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EleStoreInventoryIngestResultBO ingest(EleStoreInventoryIngestRowBO row) {
        validate(row);
        String normalizedStoreId = normalizeNullable(row.getStoreId());
        String skuCode = normalizeNullable(row.getSkuCode());
        if (StrUtil.isNotBlank(skuCode)) {
            SkuTableDO sku = skuTableMapper.selectByProductSkuCode(skuCode);
            if (sku != null && upsertFormalStock(row, normalizedStoreId, sku)) {
                EleStoreInventoryIngestResultBO result = new EleStoreInventoryIngestResultBO();
                result.setPersistStatus(PERSIST_STATUS_FORMAL);
                return result;
            }
        }
        EleSkuInventoryShadowUpsertReqBO shadowReq = buildShadowReq(row, normalizedStoreId);
        EleStoreInventoryShadowDO shadow = shadowService.upsert(shadowReq, MATCH_STATUS_SKU_NOT_MATCHED, REASON_CODE_SKU_NOT_FOUND);
        Long governanceId = governanceService.createOrRefresh(buildGovernancePool(shadowReq, shadow));
        EleStoreInventoryIngestResultBO result = new EleStoreInventoryIngestResultBO();
        result.setPersistStatus(PERSIST_STATUS_SHADOW);
        result.setReasonCode(REASON_CODE_SKU_NOT_FOUND);
        result.setShadowId(shadow == null ? null : shadow.getId());
        result.setGovernanceId(governanceId);
        return result;
    }

    private boolean upsertFormalStock(EleStoreInventoryIngestRowBO row, String normalizedStoreId, SkuTableDO sku) {
        if (StrUtil.isBlank(normalizedStoreId)) {
            return false;
        }
        StoreProductDO storeProduct = storeProductMapper.selectByStoreIdAndProductSkuId(
                normalizedStoreId, String.valueOf(sku.getProductSkuId()));
        if (storeProduct == null) {
            return false;
        }
        StoreStockDO storeStock = storeStockMapper.selectByStoreProductId(storeProduct.getStoreProductId());
        if (storeStock == null) {
            storeStock = new StoreStockDO();
            storeStock.setStoreProductId(storeProduct.getStoreProductId());
            storeStock.setStoreStockOutstockHours(0);
            fillFormalStock(row, storeStock);
            return storeStockMapper.insert(storeStock) > 0;
        }
        fillFormalStock(row, storeStock);
        return storeStockMapper.updateById(storeStock) > 0;
    }

    private void fillFormalStock(EleStoreInventoryIngestRowBO row, StoreStockDO storeStock) {
        storeStock.setStoreStockQuantity(row.getPhysicalStockTotalAmount());
        storeStock.setStoreStockAvailableQuantity(row.getAvailableForSale());
        storeStock.setStoreStockTransitQuantity(row.getPhysicalStockIntransitAmount());
        storeStock.setStoreStockFrozenQuantity(row.getReservedAmount());
    }

    private EleSkuInventoryShadowUpsertReqBO buildShadowReq(EleStoreInventoryIngestRowBO row, String normalizedStoreId) {
        EleSkuInventoryShadowUpsertReqBO reqBO = new EleSkuInventoryShadowUpsertReqBO();
        reqBO.setPlatformId(row.getPlatformId() == null ? DEFAULT_ELE_PLATFORM_ID : row.getPlatformId());
        reqBO.setMerchantCode(row.getMerchantCode());
        reqBO.setErpStoreCode(row.getErpStoreCode());
        reqBO.setPlatformStoreId(row.getPlatformStoreId());
        reqBO.setStoreId(normalizedStoreId);
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
        reqBO.setRawPayload(row.getRawPayload());
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

    private void validate(EleStoreInventoryIngestRowBO row) {
        if (row == null) {
            throw new IllegalArgumentException("库存行不能为空");
        }
        if (StrUtil.isBlank(row.getMerchantCode())) {
            throw new IllegalArgumentException("merchantCode 不能为空");
        }
        if (StrUtil.isBlank(row.getErpStoreCode())) {
            throw new IllegalArgumentException("erpStoreCode 不能为空");
        }
        if (StrUtil.isBlank(row.getSkuCode()) && StrUtil.isBlank(row.getSubSkuCode())) {
            throw new IllegalArgumentException("skuCode 和 subSkuCode 不能同时为空");
        }
    }

    private String normalizeNullable(String value) {
        String trimmedValue = StrUtil.trim(value);
        return StrUtil.isBlank(trimmedValue) ? null : trimmedValue;
    }
}
