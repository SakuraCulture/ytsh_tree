package cn.iocoder.yudao.module.business.service.warehouse;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehousePurchasePageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehousePurchaseRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehousePurchaseSaveReqVO;
import jakarta.validation.Valid;

import java.util.List;

public interface WarehousePurchaseService {

    Long createWarehousePurchase(@Valid WarehousePurchaseSaveReqVO createReqVO);

    void updateWarehousePurchase(@Valid WarehousePurchaseSaveReqVO updateReqVO);

    void deleteWarehousePurchase(Long purchaseOrderId);

    void submitWarehousePurchase(Long purchaseOrderId);

    void auditWarehousePurchase(Long purchaseOrderId);

    void confirmInbound(Long purchaseOrderId);

    void cancelWarehousePurchase(Long purchaseOrderId);

    WarehousePurchaseRespVO getWarehousePurchase(Long purchaseOrderId);

    PageResult<WarehousePurchaseRespVO> getWarehousePurchasePage(WarehousePurchasePageReqVO pageReqVO);

    List<WarehousePurchaseRespVO.Item> getWarehousePurchaseDetailList(List<Long> purchaseOrderIds);

}
