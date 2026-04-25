package cn.iocoder.yudao.module.business.controller.admin.warehouse;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.SkuSimpleRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductImportExcelVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductImportRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseProductSimpleRespVO;
import cn.iocoder.yudao.module.business.service.warehouse.WarehouseProductService;
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

@Tag(name = "管理后台 - 仓库商品")
@RestController
@RequestMapping("/warehouse-product")
@Validated
public class WarehouseProductController {

    @Resource
    private WarehouseProductService warehouseProductService;

    @PostMapping("/create")
    @Operation(summary = "创建仓库商品")
    @PreAuthorize("@ss.hasPermission('business:warehouse-product:create')")
    public CommonResult<Long> createWarehouseProduct(@Valid @RequestBody WarehouseProductSaveReqVO createReqVO) {
        return success(warehouseProductService.createWarehouseProduct(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新仓库商品")
    @PreAuthorize("@ss.hasPermission('business:warehouse-product:update')")
    public CommonResult<Boolean> updateWarehouseProduct(@Valid @RequestBody WarehouseProductSaveReqVO updateReqVO) {
        warehouseProductService.updateWarehouseProduct(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除仓库商品")
    @Parameter(name = "warehouseProductId", description = "仓库商品ID", required = true)
    @PreAuthorize("@ss.hasPermission('business:warehouse-product:delete')")
    public CommonResult<Boolean> deleteWarehouseProduct(@RequestParam("warehouseProductId") Long warehouseProductId) {
        warehouseProductService.deleteWarehouseProduct(warehouseProductId);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得仓库商品")
    @Parameter(name = "warehouseProductId", description = "仓库商品ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('business:warehouse-product:query')")
    public CommonResult<WarehouseProductRespVO> getWarehouseProduct(@RequestParam("warehouseProductId") Long warehouseProductId) {
        return success(warehouseProductService.getWarehouseProduct(warehouseProductId));
    }

    @GetMapping("/page")
    @Operation(summary = "获得仓库商品分页")
    @PreAuthorize("@ss.hasPermission('business:warehouse-product:query')")
    public CommonResult<PageResult<WarehouseProductRespVO>> getWarehouseProductPage(@Valid WarehouseProductPageReqVO pageReqVO) {
        return success(warehouseProductService.getWarehouseProductPage(pageReqVO));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得仓库商品简单列表")
    @PreAuthorize("@ss.hasPermission('business:warehouse-product:query')")
    public CommonResult<List<WarehouseProductSimpleRespVO>> getWarehouseProductSimpleList(@RequestParam("warehouseId") String warehouseId) {
        return success(warehouseProductService.getWarehouseProductSimpleList(warehouseId));
    }

    @GetMapping("/sku-simple-list")
    @Operation(summary = "获得 SKU 简单列表")
    @PreAuthorize("@ss.hasPermission('business:warehouse-product:query')")
    public CommonResult<List<SkuSimpleRespVO>> getSkuSimpleList() {
        return success(warehouseProductService.getSkuSimpleList());
    }

    @GetMapping("/export")
    @Operation(summary = "导出仓库商品 Excel")
    @PreAuthorize("@ss.hasPermission('business:warehouse-product:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWarehouseProductExcel(@Valid WarehouseProductPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<WarehouseProductRespVO> list = warehouseProductService.getWarehouseProductPage(pageReqVO).getList();
        ExcelUtils.write(response, "仓库商品.xls", "数据", WarehouseProductRespVO.class, list);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得导入门库商品模板")
    public void importTemplate(@RequestParam(value = "format", required = false, defaultValue = "excel") String format,
                               HttpServletResponse response) throws IOException {
        List<WarehouseProductImportExcelVO> list = java.util.Arrays.asList(
                WarehouseProductImportExcelVO.builder()
                        .warehouseName("默认仓库")
                        .productSkuCode("SKU001")
                        .warehouseProductCostPrice(new java.math.BigDecimal("99.99"))
                        .warehouseProductLocation("A-01-01")
                        .warehouseProductFirstDate("2024-01-01")
                        .warehouseProductLastDate("2024-06-01")
                        .build()
        );
        ExcelUtils.write(response, "仓库商品导入模板.xls", "数据", WarehouseProductImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入门店商品")
    @PreAuthorize("@ss.hasPermission('business:warehouse-product:import')")
    public CommonResult<WarehouseProductImportRespVO> importExcel(@RequestParam("file") MultipartFile file,
                                                                  @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport) throws Exception {
        List<WarehouseProductImportExcelVO> list = ExcelUtils.read(file, WarehouseProductImportExcelVO.class);
        return success(warehouseProductService.importWarehouseProductList(list, updateSupport));
    }

}
