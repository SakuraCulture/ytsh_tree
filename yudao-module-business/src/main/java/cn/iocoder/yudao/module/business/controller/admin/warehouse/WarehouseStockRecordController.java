package cn.iocoder.yudao.module.business.controller.admin.warehouse;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStockRecordPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStockRecordRespVO;
import cn.iocoder.yudao.module.business.service.warehouse.WarehouseStockRecordService;
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

@Tag(name = "管理后台 - 仓库库存流水")
@RestController
@RequestMapping("/warehouse-stock-record")
@Validated
public class WarehouseStockRecordController {

    @Resource
    private WarehouseStockRecordService warehouseStockRecordService;

    @GetMapping("/get")
    @Operation(summary = "获得仓库库存流水")
    @Parameter(name = "stockRecordId", description = "库存流水ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('business:warehouse-stock-record:query')")
    public CommonResult<WarehouseStockRecordRespVO> getWarehouseStockRecord(@RequestParam("stockRecordId") Long stockRecordId) {
        return success(warehouseStockRecordService.getWarehouseStockRecord(stockRecordId));
    }

    @GetMapping("/page")
    @Operation(summary = "获得仓库库存流水分页")
    @PreAuthorize("@ss.hasPermission('business:warehouse-stock-record:query')")
    public CommonResult<PageResult<WarehouseStockRecordRespVO>> getWarehouseStockRecordPage(@Valid WarehouseStockRecordPageReqVO pageReqVO) {
        return success(warehouseStockRecordService.getWarehouseStockRecordPage(pageReqVO));
    }

    @GetMapping("/export")
    @Operation(summary = "导出仓库库存流水 Excel")
    @PreAuthorize("@ss.hasPermission('business:warehouse-stock-record:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWarehouseStockRecordExcel(@Valid WarehouseStockRecordPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<WarehouseStockRecordRespVO> list = warehouseStockRecordService.getWarehouseStockRecordPage(pageReqVO).getList();
        ExcelUtils.write(response, "仓库库存流水.xls", "数据", WarehouseStockRecordRespVO.class, list);
    }

}
