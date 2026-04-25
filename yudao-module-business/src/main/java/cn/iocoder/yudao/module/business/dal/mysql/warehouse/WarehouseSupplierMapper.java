package cn.iocoder.yudao.module.business.dal.mysql.warehouse;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseSupplierPageReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseSupplierDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

@Mapper
public interface WarehouseSupplierMapper extends BaseMapperX<WarehouseSupplierDO> {

    default PageResult<WarehouseSupplierDO> selectPage(WarehouseSupplierPageReqVO reqVO) {
        LambdaQueryWrapperX<WarehouseSupplierDO> query = new LambdaQueryWrapperX<WarehouseSupplierDO>()
                .eqIfPresent(WarehouseSupplierDO::getCategoryName, reqVO.getCategoryName())
                .likeIfPresent(WarehouseSupplierDO::getManagerName, reqVO.getManagerName())
                .likeIfPresent(WarehouseSupplierDO::getPhone, reqVO.getPhone())
                .eqIfPresent(WarehouseSupplierDO::getSupplierStatus, reqVO.getSupplierStatus())
                .betweenIfPresent(WarehouseSupplierDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WarehouseSupplierDO::getSupplierId);
        boolean hasSupplierId = StringUtils.hasText(reqVO.getSupplierId());
        boolean hasSupplierName = StringUtils.hasText(reqVO.getSupplierName());
        if (hasSupplierId || hasSupplierName) {
            query.and(w -> {
                if (hasSupplierId) {
                    w.like(WarehouseSupplierDO::getSupplierId, reqVO.getSupplierId());
                }
                if (hasSupplierName) {
                    if (hasSupplierId) {
                        w.or();
                    }
                    w.like(WarehouseSupplierDO::getSupplierName, reqVO.getSupplierName());
                }
            });
        }
        return selectPage(reqVO, query);
    }

    default WarehouseSupplierDO selectBySupplierName(String supplierName) {
        return selectOne(WarehouseSupplierDO::getSupplierName, supplierName);
    }

    default List<WarehouseSupplierDO> selectListBySupplierStatus(Integer supplierStatus) {
        return selectList(new LambdaQueryWrapperX<WarehouseSupplierDO>()
                .eqIfPresent(WarehouseSupplierDO::getSupplierStatus, supplierStatus)
                .orderByDesc(WarehouseSupplierDO::getSupplierId));
    }

    default List<WarehouseSupplierDO> selectListBySupplierIds(Collection<String> supplierIds) {
        return selectList(new LambdaQueryWrapperX<WarehouseSupplierDO>()
                .inIfPresent(WarehouseSupplierDO::getSupplierId, supplierIds)
                .orderByDesc(WarehouseSupplierDO::getSupplierId));
    }

}
