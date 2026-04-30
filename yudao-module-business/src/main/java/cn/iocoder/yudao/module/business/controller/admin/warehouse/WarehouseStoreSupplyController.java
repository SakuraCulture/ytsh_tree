package cn.iocoder.yudao.module.business.controller.admin.warehouse;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplyImportExcelVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplyImportRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplyPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplyRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplySaveReqVO;
import cn.iocoder.yudao.module.business.service.warehouse.WarehouseStoreSupplyService;
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

@Tag(name = "管理后台 - 仓库门店供货关系")
@RestController
@RequestMapping("/warehouse-store-supply")
@Validated
public class WarehouseStoreSupplyController {

    @Resource
    private WarehouseStoreSupplyService warehouseStoreSupplyService;

    @PostMapping("/create")
    @Operation(summary = "创建仓库门店供货关系")
    @PreAuthorize("@ss.hasPermission('business:warehouse-store-supply:create')")
    public CommonResult<Long> createWarehouseStoreSupply(@Valid @RequestBody WarehouseStoreSupplySaveReqVO createReqVO) {
        return success(warehouseStoreSupplyService.createWarehouseStoreSupply(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新仓库门店供货关系")
    @PreAuthorize("@ss.hasPermission('business:warehouse-store-supply:update')")
    public CommonResult<Boolean> updateWarehouseStoreSupply(@Valid @RequestBody WarehouseStoreSupplySaveReqVO updateReqVO) {
        warehouseStoreSupplyService.updateWarehouseStoreSupply(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除仓库门店供货关系")
    @Parameter(name = "id", description = "主键", required = true)
    @PreAuthorize("@ss.hasPermission('business:warehouse-store-supply:delete')")
    public CommonResult<Boolean> deleteWarehouseStoreSupply(@RequestParam("id") Long id) {
        warehouseStoreSupplyService.deleteWarehouseStoreSupply(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得仓库门店供货关系")
    @Parameter(name = "id", description = "主键", required = true)
    @PreAuthorize("@ss.hasPermission('business:warehouse-store-supply:query')")
    public CommonResult<WarehouseStoreSupplyRespVO> getWarehouseStoreSupply(@RequestParam("id") Long id) {
        return success(warehouseStoreSupplyService.getWarehouseStoreSupply(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得仓库门店供货关系分页")
    @PreAuthorize("@ss.hasPermission('business:warehouse-store-supply:query')")
    public CommonResult<PageResult<WarehouseStoreSupplyRespVO>> getWarehouseStoreSupplyPage(@Valid WarehouseStoreSupplyPageReqVO pageReqVO) {
        return success(warehouseStoreSupplyService.getWarehouseStoreSupplyPage(pageReqVO));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得仓库门店供货关系列表")
    @Parameter(name = "warehouseId", description = "仓库ID", required = true)
    @PreAuthorize("@ss.hasPermission('business:warehouse-store-supply:query')")
    public CommonResult<List<WarehouseStoreSupplyRespVO>> getWarehouseStoreSupplySimpleList(@RequestParam("warehouseId") String warehouseId) {
        return success(warehouseStoreSupplyService.getWarehouseStoreSupplySimpleList(warehouseId));
    }

    @GetMapping("/export")
    @Operation(summary = "导出仓库门店供货关系 Excel")
    @PreAuthorize("@ss.hasPermission('business:warehouse-store-supply:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWarehouseStoreSupplyExcel(@Valid WarehouseStoreSupplyPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<WarehouseStoreSupplyRespVO> list = warehouseStoreSupplyService.getWarehouseStoreSupplyPage(pageReqVO).getList();
        ExcelUtils.write(response, "仓库门店供货关系.xls", "数据", WarehouseStoreSupplyRespVO.class, list);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得仓库门店供货关系导入模板")
    public void importTemplate(HttpServletResponse response) throws IOException {
        List<WarehouseStoreSupplyImportExcelVO> list = java.util.Collections.singletonList(
                WarehouseStoreSupplyImportExcelVO.builder()
                        .warehouseId("W001")
                        .warehouseName("默认仓")
                        .storeId("S001")
                        .storeName("示例门店")
                        .isPrimary(1)
                        .supplyStatus(1)
                        .remark("主仓供货")
                        .build()
        );
        ExcelUtils.write(response, "仓库门店供货关系导入模板.xls", "数据", WarehouseStoreSupplyImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入仓库门店供货关系")
    @PreAuthorize("@ss.hasPermission('business:warehouse-store-supply:import')")
    public CommonResult<WarehouseStoreSupplyImportRespVO> importExcel(@RequestParam("file") MultipartFile file,
                                                                       @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport) throws Exception {
        List<WarehouseStoreSupplyImportExcelVO> list = ExcelUtils.read(file, WarehouseStoreSupplyImportExcelVO.class);
        return success(warehouseStoreSupplyService.importWarehouseStoreSupplyList(list, updateSupport));
    }
}
