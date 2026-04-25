package cn.iocoder.yudao.module.business.service.warehouse;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStockRecordPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStockRecordRespVO;
import cn.iocoder.yudao.module.business.service.warehouse.bo.WarehouseStockRecordCreateReqBO;
import jakarta.validation.Valid;

public interface WarehouseStockRecordService {

    void createStockRecord(@Valid WarehouseStockRecordCreateReqBO createReqBO);

    PageResult<WarehouseStockRecordRespVO> getWarehouseStockRecordPage(WarehouseStockRecordPageReqVO pageReqVO);

    WarehouseStockRecordRespVO getWarehouseStockRecord(Long stockRecordId);

}
