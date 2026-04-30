package cn.iocoder.yudao.module.business.controller.admin.warehouse;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseLineImportExcelVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseLineImportRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseLinePageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseLineRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseLineSaveReqVO;
import cn.iocoder.yudao.module.business.service.warehouse.WarehouseLineService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 仓库线路")
@RestController
@RequestMapping("/warehouse-line")
@Validated
public class WarehouseLineController {

    @Resource
    private WarehouseLineService warehouseLineService;

    @PostMapping("/create")
    @Operation(summary = "创建仓库线路")
    @PreAuthorize("@ss.hasPermission('business:warehouse-line:create')")
    public CommonResult<Long> createWarehouseLine(@Valid @RequestBody WarehouseLineSaveReqVO createReqVO) {
        return success(warehouseLineService.createWarehouseLine(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新仓库线路")
    @PreAuthorize("@ss.hasPermission('business:warehouse-line:update')")
    public CommonResult<Boolean> updateWarehouseLine(@Valid @RequestBody WarehouseLineSaveReqVO updateReqVO) {
        warehouseLineService.updateWarehouseLine(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除仓库线路")
    @Parameter(name = "lineId", description = "线路ID", required = true)
    @PreAuthorize("@ss.hasPermission('business:warehouse-line:delete')")
    public CommonResult<Boolean> deleteWarehouseLine(@RequestParam("lineId") Long lineId) {
        warehouseLineService.deleteWarehouseLine(lineId);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得仓库线路")
    @Parameter(name = "lineId", description = "线路ID", required = true)
    @PreAuthorize("@ss.hasPermission('business:warehouse-line:query')")
    public CommonResult<WarehouseLineRespVO> getWarehouseLine(@RequestParam("lineId") Long lineId) {
        return success(warehouseLineService.getWarehouseLine(lineId));
    }

    @GetMapping("/page")
    @Operation(summary = "获得仓库线路分页")
    @PreAuthorize("@ss.hasPermission('business:warehouse-line:query')")
    public CommonResult<PageResult<WarehouseLineRespVO>> getWarehouseLinePage(@Valid WarehouseLinePageReqVO pageReqVO) {
        return success(warehouseLineService.getWarehouseLinePage(pageReqVO));
    }

    @GetMapping("/export")
    @Operation(summary = "导出仓库线路 Excel")
    @PreAuthorize("@ss.hasPermission('business:warehouse-line:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWarehouseLineExcel(@Valid WarehouseLinePageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<WarehouseLineRespVO> list = warehouseLineService.getWarehouseLinePage(pageReqVO).getList();
        ExcelUtils.write(response, "仓库线路.xls", "数据", WarehouseLineRespVO.class, list);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得仓库线路导入模板")
    public void importTemplate(HttpServletResponse response) throws IOException {
        List<WarehouseLineImportExcelVO> list = java.util.Collections.singletonList(
                WarehouseLineImportExcelVO.builder()
                        .warehouseId("W001")
                        .warehouseName("默认仓")
                        .lineCode("L001")
                        .lineName("水果线")
                        .orderWeekdays("1,3,5")
                        .lineStatus(1)
                        .storeId("S001")
                        .storeName("示例门店")
                        .sortNo(1)
                        .remark("周一三五供货")
                        .build()
        );
        ExcelUtils.write(response, "仓库线路导入模板.xls", "数据", WarehouseLineImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入仓库线路")
    @PreAuthorize("@ss.hasPermission('business:warehouse-line:import')")
    public CommonResult<WarehouseLineImportRespVO> importExcel(@RequestParam("file") MultipartFile file,
                                                                @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport) throws Exception {
        List<WarehouseLineImportExcelVO> list = ExcelUtils.read(file, WarehouseLineImportExcelVO.class);
        return success(warehouseLineService.importWarehouseLineList(list, updateSupport));
    }
}
