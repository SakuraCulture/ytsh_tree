package cn.iocoder.yudao.module.business.service.store;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreProductDO;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreProductMapper;
import cn.iocoder.yudao.module.business.service.store.bo.StoreProductSyncUpsertReqBO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Validated
public class StoreProductSyncWriteServiceImpl implements StoreProductSyncWriteService {

    @Resource
    private StoreProductMapper storeProductMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String upsertStoreProduct(@Valid StoreProductSyncUpsertReqBO reqBO) {
        StoreProductDO exist = storeProductMapper.selectByStoreIdAndProductSkuId(reqBO.getStoreId(), reqBO.getProductSkuId());
        if (exist == null) {
            StoreProductDO storeProduct = BeanUtils.toBean(reqBO, StoreProductDO.class);
            if (storeProduct.getStoreProductFirstDate() == null) {
                storeProduct.setStoreProductFirstDate(LocalDate.now());
            }
            if (storeProduct.getStoreProductShelfTime() == null) {
                storeProduct.setStoreProductShelfTime(LocalDateTime.now());
            }
            if (StrUtil.isBlank(storeProduct.getStoreProductOwnership())) {
                storeProduct.setStoreProductOwnership("入店");
            }
            storeProductMapper.insert(storeProduct);
            return storeProduct.getStoreProductId();
        }

        StoreProductDO updateObj = BeanUtils.toBean(reqBO, StoreProductDO.class);
        updateObj.setStoreProductId(exist.getStoreProductId());
        if (StrUtil.isBlank(updateObj.getStoreProductOwnership())) {
            updateObj.setStoreProductOwnership(exist.getStoreProductOwnership());
        }
        if (updateObj.getStoreProductFirstDate() == null) {
            updateObj.setStoreProductFirstDate(exist.getStoreProductFirstDate());
        }
        if (updateObj.getStoreProductShelfTime() == null) {
            updateObj.setStoreProductShelfTime(exist.getStoreProductShelfTime());
        }
        storeProductMapper.updateById(updateObj);
        return exist.getStoreProductId();
    }
}
