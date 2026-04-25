package cn.iocoder.yudao.module.business.dal.mysql.product;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SpuTableDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.*;

/**
 * SPU基础分类 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface SpuTableMapper extends BaseMapperX<SpuTableDO> {

    default PageResult<SpuTableDO> selectPage(SpuTablePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SpuTableDO>()
                .likeIfPresent(SpuTableDO::getProductSpuCode, reqVO.getProductSpuCode())
                .likeIfPresent(SpuTableDO::getProductSpuName, reqVO.getProductSpuName())
                .likeIfPresent(SpuTableDO::getProductBrand, reqVO.getProductBrand())
                .eqIfPresent(SpuTableDO::getCategoryId, reqVO.getCategoryId())
                .inIfPresent(SpuTableDO::getProductSpuId, reqVO.getSpuIds())
                .likeIfPresent(SpuTableDO::getProductOrigin, reqVO.getProductOrigin())
                .likeIfPresent(SpuTableDO::getProductManufacturer, reqVO.getProductManufacturer())
                .likeIfPresent(SpuTableDO::getProductSpecTemplate, reqVO.getProductSpecTemplate())
                .eqIfPresent(SpuTableDO::getProductSpuStatus, reqVO.getProductSpuStatus())
                .betweenIfPresent(SpuTableDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SpuTableDO::getProductSpuId));
    }

    default SpuTableDO selectByProductSpuCode(String productSpuCode) {
        return selectOne(new LambdaQueryWrapperX<SpuTableDO>()
                .eq(SpuTableDO::getProductSpuCode, productSpuCode));
    }

}