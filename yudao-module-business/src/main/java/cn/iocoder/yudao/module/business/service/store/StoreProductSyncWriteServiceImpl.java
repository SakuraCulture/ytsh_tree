package cn.iocoder.yudao.module.business.service.store;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreProductDO;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreProductMapper;
import cn.iocoder.yudao.module.business.service.store.bo.StoreProductSyncUpsertReqBO;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.annotation.Resource;
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
    public String upsertStoreProduct(StoreProductSyncUpsertReqBO reqBO) {
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
            storeProduct.setGoodsSource(1);
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
        storeProductMapper.update(new StoreProductDO(), new UpdateWrapper<StoreProductDO>()
                .eq("store_product_id", exist.getStoreProductId())
                .eq("store_id", exist.getStoreId())
                .set("store_product_ownership", updateObj.getStoreProductOwnership())
                .set("store_product_pos_status", updateObj.getStoreProductPosStatus())
                .set("store_product_price", updateObj.getStoreProductPrice())
                .set("store_product_is_active", updateObj.getStoreProductIsActive())
                .set("store_product_first_date", updateObj.getStoreProductFirstDate())
                .set("store_product_shelf_time", updateObj.getStoreProductShelfTime())
                .set("goods_source", 1));
        return exist.getStoreProductId();
    }
}
