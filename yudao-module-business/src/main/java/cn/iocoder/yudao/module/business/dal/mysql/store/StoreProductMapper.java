package cn.iocoder.yudao.module.business.dal.mysql.store;

import java.util.List;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreProductDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductPageReqVO;

/**
 * 门店商品 Mapper
 *
 * @author 彼岸花
 */
@Mapper
public interface StoreProductMapper extends BaseMapperX<StoreProductDO> {

    default PageResult<StoreProductDO> selectPage(StoreProductPageReqVO reqVO, List<String> productSkuIds) {
        return selectPage(reqVO, buildPageQuery(reqVO, productSkuIds));
    }

    default long selectCountForPage(StoreProductPageReqVO reqVO, List<String> productSkuIds) {
        return selectCount(buildBaseQuery(reqVO, productSkuIds));
    }

    default List<StoreProductDO> selectListForPage(StoreProductPageReqVO reqVO, List<String> productSkuIds) {
        return selectList(buildPageQuery(reqVO, productSkuIds));
    }

    default List<StoreProductDO> selectListForPage(StoreProductPageReqVO reqVO, List<String> productSkuIds,
                                                   int offset, int limit) {
        return selectList(buildPageQuery(reqVO, productSkuIds)
                .last("LIMIT " + offset + ", " + limit));
    }

    private LambdaQueryWrapperX<StoreProductDO> buildBaseQuery(StoreProductPageReqVO reqVO, List<String> productSkuIds) {
        return new LambdaQueryWrapperX<StoreProductDO>()
                .likeIfPresent(StoreProductDO::getStoreProductId, reqVO.getStoreProductId())
                .inIfPresent(StoreProductDO::getStoreProductId, reqVO.getStoreProductIds())
                .eqIfPresent(StoreProductDO::getStoreId, reqVO.getStoreId())
                .inIfPresent(StoreProductDO::getProductSkuId, productSkuIds)
                .eqIfPresent(StoreProductDO::getStoreProductOwnership, reqVO.getProductAttribution())
                .eqIfPresent(StoreProductDO::getStoreProductPosStatus,
                        reqVO.getPosStatus() == null ? null : String.valueOf(reqVO.getPosStatus()))
                .eqIfPresent(StoreProductDO::getStoreProductIsActive, reqVO.getEnterShopStatus())
                .betweenIfPresent(StoreProductDO::getCreateTime, reqVO.getCreateTime());
    }

    private LambdaQueryWrapperX<StoreProductDO> buildPageQuery(StoreProductPageReqVO reqVO, List<String> productSkuIds) {
        return buildBaseQuery(reqVO, productSkuIds)
                .orderByDesc(StoreProductDO::getStoreProductId);
    }

    default StoreProductDO selectByStoreIdAndProductSkuIdAndOwnership(String storeId, String productSkuId, String ownership) {
        return selectOne(new LambdaQueryWrapperX<StoreProductDO>()
                .eq(StoreProductDO::getStoreId, storeId)
                .eq(StoreProductDO::getProductSkuId, productSkuId)
                .eq(StoreProductDO::getStoreProductOwnership, ownership));
    }

    default List<StoreProductDO> selectListByStoreId(String storeId) {
        return selectList(new LambdaQueryWrapperX<StoreProductDO>()
                .eq(StoreProductDO::getStoreId, storeId)
                .orderByDesc(StoreProductDO::getStoreProductId));
    }

    default StoreProductDO selectByStoreIdAndProductSkuId(String storeId, String productSkuId) {
        return selectOne(new LambdaQueryWrapperX<StoreProductDO>()
                .eq(StoreProductDO::getStoreId, storeId)
                .eq(StoreProductDO::getProductSkuId, productSkuId)
                .orderByDesc(StoreProductDO::getStoreProductId)
                .last("LIMIT 1"));
    }

    default List<StoreProductDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<StoreProductDO>()
                .orderByDesc(StoreProductDO::getStoreProductId));
    }

}
