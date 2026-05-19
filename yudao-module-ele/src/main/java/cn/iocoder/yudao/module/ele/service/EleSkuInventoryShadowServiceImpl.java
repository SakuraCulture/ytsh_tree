package cn.iocoder.yudao.module.ele.service;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryShadowDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreInventoryShadowMapper;
import cn.iocoder.yudao.module.ele.service.bo.EleSkuInventoryShadowUpsertReqBO;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EleSkuInventoryShadowServiceImpl implements EleSkuInventoryShadowService {

    public static final String REASON_SKU_NOT_FOUND = "SKU_NOT_FOUND";
    private static final String MSG_SKU_NOT_FOUND = "未匹配到本地SKU主档";

    @Resource
    private EleStoreInventoryShadowMapper shadowMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EleStoreInventoryShadowDO upsert(EleSkuInventoryShadowUpsertReqBO reqBO, String matchStatus, String reasonCode) {
        validate(reqBO, matchStatus, reasonCode);
        EleStoreInventoryShadowDO exist = selectByAnyBizKey(reqBO.getPlatformId(), reqBO.getMerchantCode(),
                reqBO.getErpStoreCode(), reqBO.getSkuCode(), reqBO.getSubSkuCode());
        if (exist == null) {
            return insertOrRetryUpdate(reqBO, matchStatus, reasonCode);
        }
        updateExisting(exist.getId(), reqBO, matchStatus, reasonCode);
        EleStoreInventoryShadowDO updated = shadowMapper.selectById(exist.getId());
        return updated == null ? exist : updated;
    }

    private EleStoreInventoryShadowDO insertOrRetryUpdate(EleSkuInventoryShadowUpsertReqBO reqBO, String matchStatus,
                                                          String reasonCode) {
        EleStoreInventoryShadowDO row = new EleStoreInventoryShadowDO();
        fillRow(row, reqBO, matchStatus, reasonCode);
        row.setUniqueDeleted(0L);
        try {
            shadowMapper.insert(row);
            return row;
        } catch (DuplicateKeyException ex) {
            EleStoreInventoryShadowDO exist = selectByAnyBizKey(reqBO.getPlatformId(), reqBO.getMerchantCode(),
                    reqBO.getErpStoreCode(), reqBO.getSkuCode(), reqBO.getSubSkuCode());
            if (exist == null) {
                throw ex;
            }
            updateExisting(exist.getId(), reqBO, matchStatus, reasonCode);
            EleStoreInventoryShadowDO updated = shadowMapper.selectById(exist.getId());
            return updated == null ? exist : updated;
        }
    }

    private void updateExisting(Long id, EleSkuInventoryShadowUpsertReqBO reqBO, String matchStatus, String reasonCode) {
        LocalDateTime now = LocalDateTime.now();
        shadowMapper.update(new EleStoreInventoryShadowDO(), new UpdateWrapper<EleStoreInventoryShadowDO>()
                .eq("id", id)
                .set("platform_id", reqBO.getPlatformId())
                .set("merchant_code", StrUtil.trim(reqBO.getMerchantCode()))
                .set("erp_store_code", StrUtil.trim(reqBO.getErpStoreCode()))
                .set("platform_store_id", StrUtil.trim(reqBO.getPlatformStoreId()))
                .set("store_id", StrUtil.trim(reqBO.getStoreId()))
                .set("sku_code", StrUtil.trim(reqBO.getSkuCode()))
                .set("sub_sku_code", StrUtil.trim(reqBO.getSubSkuCode()))
                .set("matched_product_sku_id", StrUtil.trim(reqBO.getMatchedProductSkuId()))
                .set("matched_store_product_id", StrUtil.trim(reqBO.getMatchedStoreProductId()))
                .set("available_for_sale", reqBO.getAvailableForSale())
                .set("reserved_amount", reqBO.getReservedAmount())
                .set("physical_stock_total_amount", reqBO.getPhysicalStockTotalAmount())
                .set("physical_stock_available_amount", reqBO.getPhysicalStockAvailableAmount())
                .set("physical_stock_occupied_amount", reqBO.getPhysicalStockOccupiedAmount())
                .set("physical_stock_intransit_amount", reqBO.getPhysicalStockIntransitAmount())
                .set("owner_code", StrUtil.trim(reqBO.getOwnerCode()))
                .set("owner_name", StrUtil.trim(reqBO.getOwnerName()))
                .set("match_status", StrUtil.trim(matchStatus))
                .set("reason_code", StrUtil.trim(reasonCode))
                .set("reason_msg", resolveReasonMsg(reasonCode))
                .set("raw_payload", reqBO.getRawPayload())
                .set("last_query_time", now));
    }

    private void fillRow(EleStoreInventoryShadowDO row, EleSkuInventoryShadowUpsertReqBO reqBO,
                         String matchStatus, String reasonCode) {
        row.setPlatformId(reqBO.getPlatformId());
        row.setMerchantCode(StrUtil.trim(reqBO.getMerchantCode()));
        row.setErpStoreCode(StrUtil.trim(reqBO.getErpStoreCode()));
        row.setPlatformStoreId(StrUtil.trim(reqBO.getPlatformStoreId()));
        row.setStoreId(StrUtil.trim(reqBO.getStoreId()));
        row.setSkuCode(StrUtil.trim(reqBO.getSkuCode()));
        row.setSubSkuCode(StrUtil.trim(reqBO.getSubSkuCode()));
        row.setMatchedProductSkuId(StrUtil.trim(reqBO.getMatchedProductSkuId()));
        row.setMatchedStoreProductId(StrUtil.trim(reqBO.getMatchedStoreProductId()));
        row.setAvailableForSale(reqBO.getAvailableForSale());
        row.setReservedAmount(reqBO.getReservedAmount());
        row.setPhysicalStockTotalAmount(reqBO.getPhysicalStockTotalAmount());
        row.setPhysicalStockAvailableAmount(reqBO.getPhysicalStockAvailableAmount());
        row.setPhysicalStockOccupiedAmount(reqBO.getPhysicalStockOccupiedAmount());
        row.setPhysicalStockIntransitAmount(reqBO.getPhysicalStockIntransitAmount());
        row.setOwnerCode(StrUtil.trim(reqBO.getOwnerCode()));
        row.setOwnerName(StrUtil.trim(reqBO.getOwnerName()));
        row.setMatchStatus(StrUtil.trim(matchStatus));
        row.setReasonCode(StrUtil.trim(reasonCode));
        row.setReasonMsg(resolveReasonMsg(reasonCode));
        row.setRawPayload(reqBO.getRawPayload());
        row.setLastQueryTime(LocalDateTime.now());
    }

    private EleStoreInventoryShadowDO selectByAnyBizKey(Long platformId, String merchantCode, String erpStoreCode,
                                                         String skuCode, String subSkuCode) {
        String trimmedMerchantCode = StrUtil.trim(merchantCode);
        String trimmedErpStoreCode = StrUtil.trim(erpStoreCode);
        String trimmedSkuCode = StrUtil.trim(skuCode);
        EleStoreInventoryShadowDO skuRow = null;
        if (StrUtil.isNotBlank(trimmedSkuCode)) {
            skuRow = shadowMapper.selectBySkuCodeKey(platformId, trimmedMerchantCode, trimmedErpStoreCode, trimmedSkuCode);
        }
        String trimmedSubSkuCode = StrUtil.trim(subSkuCode);
        EleStoreInventoryShadowDO subSkuRow = null;
        if (StrUtil.isNotBlank(trimmedSubSkuCode)) {
            subSkuRow = shadowMapper.selectBySubSkuCodeKey(platformId, trimmedMerchantCode, trimmedErpStoreCode, trimmedSubSkuCode);
        }
        if (skuRow != null && subSkuRow != null && !skuRow.getId().equals(subSkuRow.getId())) {
            retireDuplicate(subSkuRow.getId());
            return skuRow;
        }
        return skuRow != null ? skuRow : subSkuRow;
    }

    private void retireDuplicate(Long id) {
        EleStoreInventoryShadowDO updateObj = new EleStoreInventoryShadowDO();
        updateObj.setId(id);
        updateObj.setUniqueDeleted(id);
        shadowMapper.updateById(updateObj);
        shadowMapper.deleteById(id);
    }

    private void validate(EleSkuInventoryShadowUpsertReqBO reqBO, String matchStatus, String reasonCode) {
        if (reqBO == null) {
            throw new IllegalArgumentException("库存影子参数不能为空");
        }
        if (reqBO.getPlatformId() == null) {
            throw new IllegalArgumentException("平台ID不能为空");
        }
        if (StrUtil.isBlank(reqBO.getMerchantCode())) {
            throw new IllegalArgumentException("商家编码不能为空");
        }
        if (StrUtil.isBlank(reqBO.getErpStoreCode())) {
            throw new IllegalArgumentException("ERP门店编码不能为空");
        }
        if (StrUtil.isBlank(reqBO.getSkuCode()) && StrUtil.isBlank(reqBO.getSubSkuCode())) {
            throw new IllegalArgumentException("skuCode 和 subSkuCode 不能同时为空");
        }
        if (StrUtil.isBlank(matchStatus)) {
            throw new IllegalArgumentException("匹配状态不能为空");
        }
        if (StrUtil.isBlank(reasonCode)) {
            throw new IllegalArgumentException("原因编码不能为空");
        }
    }

    public static String resolveReasonMsg(String reasonCode) {
        if (REASON_SKU_NOT_FOUND.equals(StrUtil.trim(reasonCode))) {
            return MSG_SKU_NOT_FOUND;
        }
        return StrUtil.blankToDefault(StrUtil.trim(reasonCode), "未知原因");
    }
}
