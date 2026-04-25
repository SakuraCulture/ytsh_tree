package cn.iocoder.yudao.module.business.service.warehouse;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseSupplierPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseSupplierSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseSupplierSimpleRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseSupplierDO;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseSupplierMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_SUPPLIER_NAME_EXISTS;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_SUPPLIER_NOT_EXISTS;

@Service
@Validated
public class WarehouseSupplierServiceImpl implements WarehouseSupplierService {

    @Resource
    private WarehouseSupplierMapper warehouseSupplierMapper;

    @Override
    public String createWarehouseSupplier(WarehouseSupplierSaveReqVO createReqVO) {
        validateWarehouseSupplierNameUnique(null, createReqVO.getSupplierName());
        WarehouseSupplierDO supplier = BeanUtils.toBean(createReqVO, WarehouseSupplierDO.class);
        warehouseSupplierMapper.insert(supplier);
        return supplier.getSupplierId();
    }

    @Override
    public void updateWarehouseSupplier(WarehouseSupplierSaveReqVO updateReqVO) {
        WarehouseSupplierDO existSupplier = validateWarehouseSupplierExists(updateReqVO.getSupplierId());
        validateWarehouseSupplierNameUnique(existSupplier.getSupplierId(), updateReqVO.getSupplierName());
        WarehouseSupplierDO updateObj = BeanUtils.toBean(updateReqVO, WarehouseSupplierDO.class);
        warehouseSupplierMapper.updateById(updateObj);
    }

    @Override
    public void deleteWarehouseSupplier(String supplierId) {
        validateWarehouseSupplierExists(supplierId);
        warehouseSupplierMapper.deleteById(supplierId);
    }

    @Override
    public WarehouseSupplierDO getWarehouseSupplier(String supplierId) {
        return warehouseSupplierMapper.selectById(supplierId);
    }

    @Override
    public PageResult<WarehouseSupplierDO> getWarehouseSupplierPage(WarehouseSupplierPageReqVO pageReqVO) {
        return warehouseSupplierMapper.selectPage(pageReqVO);
    }

    @Override
    public List<WarehouseSupplierSimpleRespVO> getWarehouseSupplierSimpleList() {
        List<WarehouseSupplierDO> list = warehouseSupplierMapper.selectListBySupplierStatus(1);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return BeanUtils.toBean(list, WarehouseSupplierSimpleRespVO.class);
    }

    @Override
    public List<WarehouseSupplierDO> getWarehouseSupplierList(Collection<String> supplierIds) {
        if (CollUtil.isEmpty(supplierIds)) {
            return Collections.emptyList();
        }
        return warehouseSupplierMapper.selectListBySupplierIds(supplierIds);
    }

    @Override
    public WarehouseSupplierDO validateWarehouseSupplierExists(String supplierId) {
        WarehouseSupplierDO supplier = warehouseSupplierMapper.selectById(supplierId);
        if (supplier == null) {
            throw exception(WAREHOUSE_SUPPLIER_NOT_EXISTS);
        }
        return supplier;
    }

    private void validateWarehouseSupplierNameUnique(String supplierId, String supplierName) {
        if (supplierName == null || supplierName.isEmpty()) {
            return;
        }
        WarehouseSupplierDO supplier = warehouseSupplierMapper.selectBySupplierName(supplierName);
        if (supplier != null && !supplier.getSupplierId().equals(supplierId)) {
            throw exception(WAREHOUSE_SUPPLIER_NAME_EXISTS);
        }
    }

}
