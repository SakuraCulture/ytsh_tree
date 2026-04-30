package cn.iocoder.yudao.module.business.service.warehouse;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplyImportExcelVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplyImportRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplyPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplyRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplySaveReqVO;
import jakarta.validation.Valid;

import java.util.List;

public interface WarehouseStoreSupplyService {

    Long createWarehouseStoreSupply(@Valid WarehouseStoreSupplySaveReqVO createReqVO);

    void updateWarehouseStoreSupply(@Valid WarehouseStoreSupplySaveReqVO updateReqVO);

    void deleteWarehouseStoreSupply(Long id);

    WarehouseStoreSupplyRespVO getWarehouseStoreSupply(Long id);

    PageResult<WarehouseStoreSupplyRespVO> getWarehouseStoreSupplyPage(WarehouseStoreSupplyPageReqVO pageReqVO);

    List<WarehouseStoreSupplyRespVO> getWarehouseStoreSupplySimpleList(String warehouseId);

    WarehouseStoreSupplyImportRespVO importWarehouseStoreSupplyList(List<WarehouseStoreSupplyImportExcelVO> importList, boolean updateSupport);
}
