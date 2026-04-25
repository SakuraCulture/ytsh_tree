package cn.iocoder.yudao.module.business.controller.admin.warehouse;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStockPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStockRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStockStatisticsRespVO;
import cn.iocoder.yudao.module.business.service.warehouse.WarehouseStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 仓库库存")
@RestController
@RequestMapping("/warehouse-stock")
@Validated
public class WarehouseStockController {

    @Resource
    private WarehouseStockService warehouseStockService;

    @GetMapping("/page")
    @Operation(summary = "获得仓库库存分页")
    @PreAuthorize("@ss.hasPermission('business:warehouse-stock:query')")
    public CommonResult<?> getWarehouseStockPage(@Valid WarehouseStockPageReqVO pageReqVO) {
        return success(warehouseStockService.getWarehouseStockPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得仓库库存")
    @Parameter(name = "warehouseStockId", description = "仓库库存ID", required = true)
    @PreAuthorize("@ss.hasPermission('business:warehouse-stock:query')")
    public CommonResult<WarehouseStockRespVO> getWarehouseStock(@RequestParam("warehouseStockId") Long warehouseStockId) {
        return success(warehouseStockService.getWarehouseStock(warehouseStockId));
    }

    @GetMapping("/statistics")
    @Operation(summary = "获得仓库库存统计")
    @PreAuthorize("@ss.hasPermission('business:warehouse-stock:query')")
    public CommonResult<WarehouseStockStatisticsRespVO> getWarehouseStockStatistics(@Valid WarehouseStockPageReqVO pageReqVO) {
        return success(warehouseStockService.getWarehouseStockStatistics(pageReqVO));
    }

    @GetMapping("/export")
    @Operation(summary = "导出仓库库存 Excel")
    @PreAuthorize("@ss.hasPermission('business:warehouse-stock:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWarehouseStockExcel(@Valid WarehouseStockPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<WarehouseStockRespVO> list = warehouseStockService.getWarehouseStockPage(pageReqVO).getList();
        ExcelUtils.write(response, "仓库库存.xls", "数据", WarehouseStockRespVO.class, list);
    }

}
