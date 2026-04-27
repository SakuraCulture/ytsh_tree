package cn.iocoder.yudao.module.ele.service;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsShadowDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsShadowMapper;
import cn.iocoder.yudao.module.ele.enums.EleStoreGoodsShadowStatus;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsShadowUpsertReqBO;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class EleStoreGoodsShadowServiceImpl implements EleStoreGoodsShadowService {

    private static final Set<String> MATCH_STATUSES = Set.of(
            EleStoreGoodsShadowStatus.UNMATCHED,
            EleStoreGoodsShadowStatus.MATCHED,
            EleStoreGoodsShadowStatus.MERGED,
            EleStoreGoodsShadowStatus.CONFLICT,
            EleStoreGoodsShadowStatus.IGNORED);

    @Resource
    private EleStoreGoodsShadowMapper shadowMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EleStoreGoodsShadowDO upsertFromSync(EleStoreGoodsShadowUpsertReqBO reqBO, String matchStatus,
                                                String matchedProductSkuId, String mergedStoreProductId) {
        validateUpsert(reqBO, matchStatus);
        String merchantCode = StrUtil.trim(reqBO.getMerchantCode());
        String erpStoreCode = StrUtil.trim(reqBO.getErpStoreCode());
        String skuCode = StrUtil.trim(reqBO.getSkuCode());
        EleStoreGoodsShadowDO exist = shadowMapper.selectByBizKey(reqBO.getPlatformId(), merchantCode, erpStoreCode, skuCode);
        if (exist == null) {
            return insertOrRetryUpdate(reqBO, matchStatus, matchedProductSkuId, mergedStoreProductId,
                    merchantCode, erpStoreCode, skuCode);
        }
        updateExisting(exist.getId(), reqBO, matchStatus, matchedProductSkuId, mergedStoreProductId);
        EleStoreGoodsShadowDO updated = shadowMapper.selectById(exist.getId());
        return updated == null ? exist : updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markMerged(Long shadowId, String matchedProductSkuId, String mergedStoreProductId) {
        if (shadowId == null) {
            throw new IllegalArgumentException("影子门店品ID不能为空");
        }
        if (StrUtil.isBlank(matchedProductSkuId)) {
            throw new IllegalArgumentException("匹配SKU ID不能为空");
        }
        if (StrUtil.isBlank(mergedStoreProductId)) {
            throw new IllegalArgumentException("正式门店商品ID不能为空");
        }
        EleStoreGoodsShadowDO exist = shadowMapper.selectById(shadowId);
        if (exist == null) {
            throw new IllegalArgumentException("影子门店品不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        shadowMapper.update(new EleStoreGoodsShadowDO(), new UpdateWrapper<EleStoreGoodsShadowDO>()
                .eq("id", shadowId)
                .set("match_status", EleStoreGoodsShadowStatus.MERGED)
                .set("matched_product_sku_id", StrUtil.trim(matchedProductSkuId))
                .set("merged_store_product_id", StrUtil.trim(mergedStoreProductId))
                .set("matched_time", now)
                .set("merged_time", now));
    }

    private EleStoreGoodsShadowDO insertOrRetryUpdate(EleStoreGoodsShadowUpsertReqBO reqBO, String matchStatus,
                                                      String matchedProductSkuId, String mergedStoreProductId,
                                                      String merchantCode, String erpStoreCode, String skuCode) {
        EleStoreGoodsShadowDO row = new EleStoreGoodsShadowDO();
        fillFromReq(row, reqBO);
        fillMatch(row, matchStatus, matchedProductSkuId, mergedStoreProductId, LocalDateTime.now());
        row.setUniqueDeleted(0L);
        try {
            shadowMapper.insert(row);
            return row;
        } catch (DuplicateKeyException ex) {
            EleStoreGoodsShadowDO exist = shadowMapper.selectByBizKey(reqBO.getPlatformId(), merchantCode, erpStoreCode, skuCode);
            if (exist == null) {
                throw ex;
            }
            updateExisting(exist.getId(), reqBO, matchStatus, matchedProductSkuId, mergedStoreProductId);
            EleStoreGoodsShadowDO updated = shadowMapper.selectById(exist.getId());
            return updated == null ? exist : updated;
        }
    }

    private void updateExisting(Long id, EleStoreGoodsShadowUpsertReqBO reqBO, String matchStatus,
                                String matchedProductSkuId, String mergedStoreProductId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime matchedTime = null;
        LocalDateTime mergedTime = null;
        if (EleStoreGoodsShadowStatus.MERGED.equals(matchStatus)) {
            matchedTime = now;
            mergedTime = now;
        } else if (StrUtil.isNotBlank(matchedProductSkuId)) {
            matchedTime = now;
        }
        shadowMapper.update(new EleStoreGoodsShadowDO(), new UpdateWrapper<EleStoreGoodsShadowDO>()
                .eq("id", id)
                .set("platform_id", reqBO.getPlatformId())
                .set("merchant_code", StrUtil.trim(reqBO.getMerchantCode()))
                .set("erp_store_code", StrUtil.trim(reqBO.getErpStoreCode()))
                .set("platform_store_id", StrUtil.trim(reqBO.getPlatformStoreId()))
                .set("store_id", StrUtil.trim(reqBO.getStoreId()))
                .set("spu_code", StrUtil.trim(reqBO.getSpuCode()))
                .set("sku_code", StrUtil.trim(reqBO.getSkuCode()))
                .set("sub_sku_code", StrUtil.trim(reqBO.getSubSkuCode()))
                .set("title", StrUtil.trim(reqBO.getTitle()))
                .set("main_pic", StrUtil.trim(reqBO.getMainPic()))
                .set("sub_pics", reqBO.getSubPics())
                .set("front_category", reqBO.getFrontCategory())
                .set("brand_name", StrUtil.trim(reqBO.getBrandName()))
                .set("specification", StrUtil.trim(reqBO.getSpecification()))
                .set("sale_price", reqBO.getSalePrice())
                .set("pos_status", StrUtil.trim(reqBO.getPosStatus()))
                .set("is_active", reqBO.getIsActive())
                .set("raw_payload", reqBO.getRawPayload())
                .set("match_status", matchStatus)
                .set("matched_product_sku_id", StrUtil.trim(matchedProductSkuId))
                .set("merged_store_product_id", StrUtil.trim(mergedStoreProductId))
                .set("last_sync_time", now)
                .set("matched_time", matchedTime)
                .set("merged_time", mergedTime)
                .set("conflict_reason", null));
    }

    private void validateUpsert(EleStoreGoodsShadowUpsertReqBO reqBO, String matchStatus) {
        if (reqBO == null) {
            throw new IllegalArgumentException("影子门店品同步参数不能为空");
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
        if (StrUtil.isBlank(reqBO.getSkuCode())) {
            throw new IllegalArgumentException("SKU编码不能为空");
        }
        if (!MATCH_STATUSES.contains(matchStatus)) {
            throw new IllegalArgumentException("影子门店品匹配状态无效");
        }
    }

    private void fillFromReq(EleStoreGoodsShadowDO row, EleStoreGoodsShadowUpsertReqBO reqBO) {
        row.setPlatformId(reqBO.getPlatformId());
        row.setMerchantCode(StrUtil.trim(reqBO.getMerchantCode()));
        row.setErpStoreCode(StrUtil.trim(reqBO.getErpStoreCode()));
        row.setPlatformStoreId(StrUtil.trim(reqBO.getPlatformStoreId()));
        row.setStoreId(StrUtil.trim(reqBO.getStoreId()));
        row.setSpuCode(StrUtil.trim(reqBO.getSpuCode()));
        row.setSkuCode(StrUtil.trim(reqBO.getSkuCode()));
        row.setSubSkuCode(StrUtil.trim(reqBO.getSubSkuCode()));
        row.setTitle(StrUtil.trim(reqBO.getTitle()));
        row.setMainPic(StrUtil.trim(reqBO.getMainPic()));
        row.setSubPics(reqBO.getSubPics());
        row.setFrontCategory(reqBO.getFrontCategory());
        row.setBrandName(StrUtil.trim(reqBO.getBrandName()));
        row.setSpecification(StrUtil.trim(reqBO.getSpecification()));
        row.setSalePrice(reqBO.getSalePrice());
        row.setPosStatus(StrUtil.trim(reqBO.getPosStatus()));
        row.setIsActive(reqBO.getIsActive());
        row.setRawPayload(reqBO.getRawPayload());
    }

    private void fillMatch(EleStoreGoodsShadowDO row, String matchStatus, String matchedProductSkuId,
                           String mergedStoreProductId, LocalDateTime now) {
        row.setMatchStatus(matchStatus);
        row.setMatchedProductSkuId(StrUtil.trim(matchedProductSkuId));
        row.setMergedStoreProductId(StrUtil.trim(mergedStoreProductId));
        row.setLastSyncTime(now);
        if (EleStoreGoodsShadowStatus.MERGED.equals(matchStatus)) {
            row.setMatchedTime(now);
            row.setMergedTime(now);
        } else if (StrUtil.isNotBlank(matchedProductSkuId)) {
            row.setMatchedTime(now);
        }
    }
}
