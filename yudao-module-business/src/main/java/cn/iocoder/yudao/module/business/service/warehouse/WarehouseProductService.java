package cn.iocoder.yudao.module.business.service.warehouse;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.SkuSimpleRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductImportExcelVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductImportRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductSimpleRespVO;
import jakarta.validation.Valid;

import java.util.List;

public interface WarehouseProductService {

    Long createWarehouseProduct(@Valid WarehouseProductSaveReqVO createReqVO);

    void updateWarehouseProduct(@Valid WarehouseProductSaveReqVO updateReqVO);

    void deleteWarehouseProduct(Long warehouseProductId);

    WarehouseProductRespVO getWarehouseProduct(Long warehouseProductId);

    PageResult<WarehouseProductRespVO> getWarehouseProductPage(WarehouseProductPageReqVO pageReqVO);

    List<WarehouseProductSimpleRespVO> getWarehouseProductSimpleList(String warehouseId);

    List<SkuSimpleRespVO> getSkuSimpleList();

    WarehouseProductImportRespVO importWarehouseProductList(List<WarehouseProductImportExcelVO> importList, boolean updateSupport);

}
