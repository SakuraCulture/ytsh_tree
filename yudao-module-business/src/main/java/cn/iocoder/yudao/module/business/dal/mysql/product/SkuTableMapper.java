package cn.iocoder.yudao.module.business.dal.mysql.product;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * SKU商品主数据 Mapper
 *
 * @author 彼岸花
 */
@Mapper
public interface SkuTableMapper extends BaseMapperX<SkuTableDO> {

    default List<SkuTableDO> selectListByProductSpuId(Long productSpuId) {
        return selectList(SkuTableDO::getProductSpuId, productSpuId);
    }

    default List<SkuTableDO> selectListByProductSpuIds(Collection<Long> productSpuIds) {
        return selectList(new LambdaQueryWrapperX<SkuTableDO>()
                .inIfPresent(SkuTableDO::getProductSpuId, productSpuIds)
                .orderByDesc(SkuTableDO::getProductSpuId));
    }

    default SkuTableDO selectByProductSkuCode(String productSkuCode) {
        return selectOne(new LambdaQueryWrapperX<SkuTableDO>()
                .eq(SkuTableDO::getProductSkuCode, productSkuCode));
    }

    default List<SkuTableDO> selectListByProductSkuIds(Collection<String> productSkuIds) {
        return selectList(new LambdaQueryWrapperX<SkuTableDO>()
                .inIfPresent(SkuTableDO::getProductSkuId, productSkuIds)
                .orderByDesc(SkuTableDO::getProductSkuId));
    }

    default List<SkuTableDO> selectListByKeyword(String productSkuCode, String productSkuName) {
        return selectList(new LambdaQueryWrapperX<SkuTableDO>()
                .likeIfPresent(SkuTableDO::getProductSkuCode, productSkuCode)
                .likeIfPresent(SkuTableDO::getProductSkuName, productSkuName)
                .orderByDesc(SkuTableDO::getProductSkuId));
    }

    default int deleteByProductSpuId(Long productSpuId) {
        return delete(SkuTableDO::getProductSpuId, productSpuId);
    }

	default int deleteByProductSpuIds(List<Long> productSpuIds) {
	    return deleteBatch(SkuTableDO::getProductSpuId, productSpuIds);
	}

	default List<SkuTableDO> selectAllSimpleList() {
	    return selectList(new LambdaQueryWrapperX<SkuTableDO>()
	            .orderByDesc(SkuTableDO::getProductSkuId));
	}

}