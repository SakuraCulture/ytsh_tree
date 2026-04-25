package cn.iocoder.yudao.module.business.controller.admin.warehouse;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseSupplierPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseSupplierRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseSupplierSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseSupplierSimpleRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseSupplierDO;
import cn.iocoder.yudao.module.business.service.warehouse.WarehouseSupplierService;
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

@Tag(name = "管理后台 - 仓库供应商")
@RestController
@RequestMapping("/warehouse-supplier")
@Validated
public class WarehouseSupplierController {

    @Resource
    private WarehouseSupplierService warehouseSupplierService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商")
    @PreAuthorize("@ss.hasPermission('business:warehouse-supplier:create')")
    public CommonResult<String> createWarehouseSupplier(@Valid @RequestBody WarehouseSupplierSaveReqVO createReqVO) {
        return success(warehouseSupplierService.createWarehouseSupplier(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商")
    @PreAuthorize("@ss.hasPermission('business:warehouse-supplier:update')")
    public CommonResult<Boolean> updateWarehouseSupplier(@Valid @RequestBody WarehouseSupplierSaveReqVO updateReqVO) {
        warehouseSupplierService.updateWarehouseSupplier(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商")
    @Parameter(name = "supplierId", description = "供应商ID", required = true)
    @PreAuthorize("@ss.hasPermission('business:warehouse-supplier:delete')")
    public CommonResult<Boolean> deleteWarehouseSupplier(@RequestParam("supplierId") String supplierId) {
        warehouseSupplierService.deleteWarehouseSupplier(supplierId);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商")
    @Parameter(name = "supplierId", description = "供应商ID", required = true, example = "SUP001")
    @PreAuthorize("@ss.hasPermission('business:warehouse-supplier:query')")
    public CommonResult<WarehouseSupplierRespVO> getWarehouseSupplier(@RequestParam("supplierId") String supplierId) {
        WarehouseSupplierDO supplier = warehouseSupplierService.getWarehouseSupplier(supplierId);
        return success(BeanUtils.toBean(supplier, WarehouseSupplierRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得供应商分页")
    @PreAuthorize("@ss.hasPermission('business:warehouse-supplier:query')")
    public CommonResult<PageResult<WarehouseSupplierRespVO>> getWarehouseSupplierPage(@Valid WarehouseSupplierPageReqVO pageReqVO) {
        PageResult<WarehouseSupplierDO> pageResult = warehouseSupplierService.getWarehouseSupplierPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, WarehouseSupplierRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得供应商简单列表")
    @PreAuthorize("@ss.hasPermission('business:warehouse-supplier:query')")
    public CommonResult<List<WarehouseSupplierSimpleRespVO>> getWarehouseSupplierSimpleList() {
        return success(warehouseSupplierService.getWarehouseSupplierSimpleList());
    }

    @GetMapping("/export")
    @Operation(summary = "导出供应商 Excel")
    @PreAuthorize("@ss.hasPermission('business:warehouse-supplier:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWarehouseSupplierExcel(@Valid WarehouseSupplierPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<WarehouseSupplierDO> list = warehouseSupplierService.getWarehouseSupplierPage(pageReqVO).getList();
        ExcelUtils.write(response, "供应商.xls", "数据", WarehouseSupplierRespVO.class, BeanUtils.toBean(list, WarehouseSupplierRespVO.class));
    }

}
