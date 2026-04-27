package cn.iocoder.yudao.module.business.dal.mysql.product;

import java.util.*;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.UpcTablePageReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.UpcTableDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UpcTableMapper extends BaseMapperX<UpcTableDO> {

    default List<UpcTableDO> selectListByProductSkuId(Long productSkuId) {
        return selectList(UpcTableDO::getProductSkuId, productSkuId);
    }

    default List<UpcTableDO> selectListByProductSkuIds(Collection<Long> productSkuIds) {
        return selectList(new LambdaQueryWrapperX<UpcTableDO>()
                .inIfPresent(UpcTableDO::getProductSkuId, productSkuIds)
                .orderByAsc(UpcTableDO::getProductSkuId)
                .orderByAsc(UpcTableDO::getProductUpcId));
    }

    default int deleteByProductSkuId(Long productSkuId) {
        return delete(UpcTableDO::getProductSkuId, productSkuId);
    }

    default PageResult<UpcTableDO> selectPage(UpcTablePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<UpcTableDO>()
                .eqIfPresent(UpcTableDO::getProductSkuId, reqVO.getProductSkuId())
                .eqIfPresent(UpcTableDO::getProductUpcType, reqVO.getProductUpcType())
                .eqIfPresent(UpcTableDO::getProductUpcStatus, reqVO.getProductUpcStatus())
                .orderByDesc(UpcTableDO::getProductUpcId));
    }

}
