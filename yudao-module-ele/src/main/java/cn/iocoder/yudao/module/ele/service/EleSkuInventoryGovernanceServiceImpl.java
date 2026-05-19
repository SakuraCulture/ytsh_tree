package cn.iocoder.yudao.module.ele.service;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryGovernancePoolDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreInventoryGovernancePoolMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EleSkuInventoryGovernanceServiceImpl implements EleSkuInventoryGovernanceService {

    public static final String STATUS_PENDING = "PENDING";

    @Resource
    private EleStoreInventoryGovernancePoolMapper governancePoolMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrRefresh(EleStoreInventoryGovernancePoolDO governancePool) {
        validate(governancePool);
        EleStoreInventoryGovernancePoolDO exist = selectByAnyBizKey(governancePool);
        if (exist == null) {
            governancePool.setProcessStatus(StrUtil.blankToDefault(StrUtil.trim(governancePool.getProcessStatus()), STATUS_PENDING));
            governancePool.setUniqueDeleted(0L);
            try {
                governancePoolMapper.insert(governancePool);
                return governancePool.getId();
            } catch (DuplicateKeyException ex) {
                exist = selectByAnyBizKey(governancePool);
                if (exist == null) {
                    throw ex;
                }
            }
        }
        governancePoolMapper.update(new EleStoreInventoryGovernancePoolDO(), new UpdateWrapper<EleStoreInventoryGovernancePoolDO>()
                .eq("id", exist.getId())
                .set("platform_id", governancePool.getPlatformId())
                .set("merchant_code", StrUtil.trim(governancePool.getMerchantCode()))
                .set("erp_store_code", StrUtil.trim(governancePool.getErpStoreCode()))
                .set("platform_store_id", StrUtil.trim(governancePool.getPlatformStoreId()))
                .set("store_id", StrUtil.trim(governancePool.getStoreId()))
                .set("sku_code", StrUtil.trim(governancePool.getSkuCode()))
                .set("sub_sku_code", StrUtil.trim(governancePool.getSubSkuCode()))
                .set("inventory_shadow_id", governancePool.getInventoryShadowId())
                .set("reason_code", StrUtil.trim(governancePool.getReasonCode()))
                .set("reason_msg", StrUtil.trim(governancePool.getReasonMsg()))
                .set("process_status", STATUS_PENDING)
                .set("raw_payload", governancePool.getRawPayload())
                .set("remark", StrUtil.trim(governancePool.getRemark())));
        return exist.getId();
    }

    private EleStoreInventoryGovernancePoolDO selectByAnyBizKey(EleStoreInventoryGovernancePoolDO governancePool) {
        String trimmedMerchantCode = StrUtil.trim(governancePool.getMerchantCode());
        String trimmedErpStoreCode = StrUtil.trim(governancePool.getErpStoreCode());
        String trimmedReasonCode = StrUtil.trim(governancePool.getReasonCode());
        String trimmedSkuCode = StrUtil.trim(governancePool.getSkuCode());
        EleStoreInventoryGovernancePoolDO skuRow = null;
        if (StrUtil.isNotBlank(trimmedSkuCode)) {
            skuRow = governancePoolMapper.selectBySkuCodeKey(governancePool.getPlatformId(),
                    trimmedMerchantCode, trimmedErpStoreCode, trimmedSkuCode, trimmedReasonCode);
        }
        String trimmedSubSkuCode = StrUtil.trim(governancePool.getSubSkuCode());
        EleStoreInventoryGovernancePoolDO subSkuRow = null;
        if (StrUtil.isNotBlank(trimmedSubSkuCode)) {
            subSkuRow = governancePoolMapper.selectBySubSkuCodeKey(governancePool.getPlatformId(),
                    trimmedMerchantCode, trimmedErpStoreCode, trimmedSubSkuCode, trimmedReasonCode);
        }
        if (skuRow != null && subSkuRow != null && !skuRow.getId().equals(subSkuRow.getId())) {
            retireDuplicate(subSkuRow.getId());
            return skuRow;
        }
        return skuRow != null ? skuRow : subSkuRow;
    }

    private void retireDuplicate(Long id) {
        EleStoreInventoryGovernancePoolDO updateObj = new EleStoreInventoryGovernancePoolDO();
        updateObj.setId(id);
        updateObj.setUniqueDeleted(id);
        governancePoolMapper.updateById(updateObj);
        governancePoolMapper.deleteById(id);
    }

    private void validate(EleStoreInventoryGovernancePoolDO governancePool) {
        if (governancePool == null) {
            throw new IllegalArgumentException("库存治理记录不能为空");
        }
        if (governancePool.getPlatformId() == null) {
            throw new IllegalArgumentException("平台ID不能为空");
        }
        if (StrUtil.isBlank(governancePool.getMerchantCode())) {
            throw new IllegalArgumentException("商家编码不能为空");
        }
        if (StrUtil.isBlank(governancePool.getErpStoreCode())) {
            throw new IllegalArgumentException("ERP门店编码不能为空");
        }
        if (StrUtil.isBlank(governancePool.getSkuCode()) && StrUtil.isBlank(governancePool.getSubSkuCode())) {
            throw new IllegalArgumentException("skuCode 和 subSkuCode 不能同时为空");
        }
        if (StrUtil.isBlank(governancePool.getReasonCode())) {
            throw new IllegalArgumentException("原因编码不能为空");
        }
        if (StrUtil.isBlank(governancePool.getReasonMsg())) {
            throw new IllegalArgumentException("原因描述不能为空");
        }
    }
}
