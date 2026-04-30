package cn.iocoder.yudao.module.business.service.warehouse;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseLineImportExcelVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseLineImportRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseLinePageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseLineRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseLineSaveReqVO;
import jakarta.validation.Valid;

import java.util.List;

public interface WarehouseLineService {

    Long createWarehouseLine(@Valid WarehouseLineSaveReqVO createReqVO);

    void updateWarehouseLine(@Valid WarehouseLineSaveReqVO updateReqVO);

    void deleteWarehouseLine(Long lineId);

    WarehouseLineRespVO getWarehouseLine(Long lineId);

    PageResult<WarehouseLineRespVO> getWarehouseLinePage(WarehouseLinePageReqVO pageReqVO);

    WarehouseLineImportRespVO importWarehouseLineList(List<WarehouseLineImportExcelVO> importList, boolean updateSupport);
}
