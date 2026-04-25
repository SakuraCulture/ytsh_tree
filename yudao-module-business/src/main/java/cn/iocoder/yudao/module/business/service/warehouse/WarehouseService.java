package cn.iocoder.yudao.module.business.service.warehouse;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehousePageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseSimpleRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

public interface WarehouseService {

    String createWarehouse(@Valid WarehouseSaveReqVO createReqVO);

    void updateWarehouse(@Valid WarehouseSaveReqVO updateReqVO);

    void updateWarehouseDefaultStatus(String warehouseId, Integer isDefault);

    void deleteWarehouse(String warehouseId);

    WarehouseDO getWarehouse(String warehouseId);

    PageResult<WarehouseDO> getWarehousePage(WarehousePageReqVO pageReqVO);

    List<WarehouseSimpleRespVO> getWarehouseSimpleList();

    List<WarehouseDO> getWarehouseList(Collection<String> warehouseIds);

    WarehouseDO validateWarehouseExists(String warehouseId);

}
