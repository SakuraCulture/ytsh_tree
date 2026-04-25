package cn.iocoder.yudao.module.business.service.warehouse;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseSupplierPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseSupplierSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseSupplierSimpleRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseSupplierDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

public interface WarehouseSupplierService {

    String createWarehouseSupplier(@Valid WarehouseSupplierSaveReqVO createReqVO);

    void updateWarehouseSupplier(@Valid WarehouseSupplierSaveReqVO updateReqVO);

    void deleteWarehouseSupplier(String supplierId);

    WarehouseSupplierDO getWarehouseSupplier(String supplierId);

    PageResult<WarehouseSupplierDO> getWarehouseSupplierPage(WarehouseSupplierPageReqVO pageReqVO);

    List<WarehouseSupplierSimpleRespVO> getWarehouseSupplierSimpleList();

    List<WarehouseSupplierDO> getWarehouseSupplierList(Collection<String> supplierIds);

    WarehouseSupplierDO validateWarehouseSupplierExists(String supplierId);

}
