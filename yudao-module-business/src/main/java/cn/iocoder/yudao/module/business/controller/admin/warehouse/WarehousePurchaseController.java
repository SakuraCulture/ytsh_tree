package cn.iocoder.yudao.module.business.controller.admin.warehouse;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehousePurchasePageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehousePurchaseRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehousePurchaseSaveReqVO;
import cn.iocoder.yudao.module.business.service.warehouse.WarehousePurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 仓库采购订单")
@RestController
@RequestMapping("/warehouse-purchase")
@Validated
public class WarehousePurchaseController {

    @Resource
    private WarehousePurchaseService warehousePurchaseService;

    @PostMapping("/create")
    @Operation(summary = "创建仓库采购订单")
    @PreAuthorize("@ss.hasPermission('business:warehouse-purchase:create')")
    public CommonResult<Long> createWarehousePurchase(@Valid @RequestBody WarehousePurchaseSaveReqVO createReqVO) {
        return success(warehousePurchaseService.createWarehousePurchase(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新仓库采购订单")
    @PreAuthorize("@ss.hasPermission('business:warehouse-purchase:update')")
    public CommonResult<Boolean> updateWarehousePurchase(@Valid @RequestBody WarehousePurchaseSaveReqVO updateReqVO) {
        warehousePurchaseService.updateWarehousePurchase(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除仓库采购订单")
    @Parameter(name = "purchaseOrderId", description = "采购订单ID", required = true)
    @PreAuthorize("@ss.hasPermission('business:warehouse-purchase:delete')")
    public CommonResult<Boolean> deleteWarehousePurchase(@RequestParam("purchaseOrderId") Long purchaseOrderId) {
        warehousePurchaseService.deleteWarehousePurchase(purchaseOrderId);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交仓库采购订单")
    @Parameter(name = "purchaseOrderId", description = "采购订单ID", required = true)
    @PreAuthorize("@ss.hasPermission('business:warehouse-purchase:update')")
    public CommonResult<Boolean> submitWarehousePurchase(@RequestParam("purchaseOrderId") Long purchaseOrderId) {
        warehousePurchaseService.submitWarehousePurchase(purchaseOrderId);
        return success(true);
    }

    @PutMapping("/audit")
    @Operation(summary = "审核仓库采购订单")
    @Parameter(name = "purchaseOrderId", description = "采购订单ID", required = true)
    @PreAuthorize("@ss.hasPermission('business:warehouse-purchase:update')")
    public CommonResult<Boolean> auditWarehousePurchase(@RequestParam("purchaseOrderId") Long purchaseOrderId) {
        warehousePurchaseService.auditWarehousePurchase(purchaseOrderId);
        return success(true);
    }

    @PutMapping("/confirm-inbound")
    @Operation(summary = "确认仓库采购订单入库")
    @Parameter(name = "purchaseOrderId", description = "采购订单ID", required = true)
    @PreAuthorize("@ss.hasPermission('business:warehouse-purchase:update')")
    public CommonResult<Boolean> confirmInbound(@RequestParam("purchaseOrderId") Long purchaseOrderId) {
        warehousePurchaseService.confirmInbound(purchaseOrderId);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消仓库采购订单")
    @Parameter(name = "purchaseOrderId", description = "采购订单ID", required = true)
    @PreAuthorize("@ss.hasPermission('business:warehouse-purchase:update')")
    public CommonResult<Boolean> cancelWarehousePurchase(@RequestParam("purchaseOrderId") Long purchaseOrderId) {
        warehousePurchaseService.cancelWarehousePurchase(purchaseOrderId);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得仓库采购订单")
    @Parameter(name = "purchaseOrderId", description = "采购订单ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('business:warehouse-purchase:query')")
    public CommonResult<WarehousePurchaseRespVO> getWarehousePurchase(@RequestParam("purchaseOrderId") Long purchaseOrderId) {
        return success(warehousePurchaseService.getWarehousePurchase(purchaseOrderId));
    }

    @GetMapping("/page")
    @Operation(summary = "获得仓库采购订单分页")
    @PreAuthorize("@ss.hasPermission('business:warehouse-purchase:query')")
    public CommonResult<PageResult<WarehousePurchaseRespVO>> getWarehousePurchasePage(@Valid WarehousePurchasePageReqVO pageReqVO) {
        return success(warehousePurchaseService.getWarehousePurchasePage(pageReqVO));
    }

    @GetMapping("/export")
    @Operation(summary = "导出仓库采购订单 Excel")
    @PreAuthorize("@ss.hasPermission('business:warehouse-purchase:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWarehousePurchaseExcel(@Valid WarehousePurchasePageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<WarehousePurchaseRespVO> list = warehousePurchaseService.getWarehousePurchasePage(pageReqVO).getList();
        ExcelUtils.write(response, "仓库采购订单.xls", "数据", WarehousePurchaseRespVO.class, list);
    }

}
