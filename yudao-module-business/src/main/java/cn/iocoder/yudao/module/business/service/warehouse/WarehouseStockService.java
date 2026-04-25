package cn.iocoder.yudao.module.business.service.warehouse;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStockPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStockRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStockStatisticsRespVO;

public interface WarehouseStockService {

    PageResult<WarehouseStockRespVO> getWarehouseStockPage(WarehouseStockPageReqVO pageReqVO);

    WarehouseStockRespVO getWarehouseStock(Long warehouseStockId);

    WarehouseStockStatisticsRespVO getWarehouseStockStatistics(WarehouseStockPageReqVO pageReqVO);

}
