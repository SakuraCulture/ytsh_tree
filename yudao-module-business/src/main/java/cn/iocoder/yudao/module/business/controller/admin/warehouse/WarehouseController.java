package cn.iocoder.yudao.module.business.controller.admin.warehouse;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehousePageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseSimpleRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseDO;
import cn.iocoder.yudao.module.business.service.warehouse.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
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

@Tag(name = "管理后台 - 仓库")
@RestController
@RequestMapping("/warehouse")
@Validated
public class WarehouseController {

    @Resource
    private WarehouseService warehouseService;

    @PostMapping("/create")
    @Operation(summary = "创建仓库")
    @PreAuthorize("@ss.hasPermission('business:warehouse:create')")
    public CommonResult<String> createWarehouse(@Valid @RequestBody WarehouseSaveReqVO createReqVO) {
        return success(warehouseService.createWarehouse(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新仓库")
    @PreAuthorize("@ss.hasPermission('business:warehouse:update')")
    public CommonResult<Boolean> updateWarehouse(@Valid @RequestBody WarehouseSaveReqVO updateReqVO) {
        warehouseService.updateWarehouse(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-default-status")
    @Operation(summary = "更新默认仓状态")
    @Parameters({
            @Parameter(name = "warehouseId", description = "仓库ID", required = true),
            @Parameter(name = "isDefault", description = "是否默认仓(0否1是)", required = true)
    })
    @PreAuthorize("@ss.hasPermission('business:warehouse:update')")
    public CommonResult<Boolean> updateWarehouseDefaultStatus(@RequestParam("warehouseId") String warehouseId,
                                                              @RequestParam("isDefault") Integer isDefault) {
        warehouseService.updateWarehouseDefaultStatus(warehouseId, isDefault);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除仓库")
    @Parameter(name = "warehouseId", description = "仓库ID", required = true)
    @PreAuthorize("@ss.hasPermission('business:warehouse:delete')")
    public CommonResult<Boolean> deleteWarehouse(@RequestParam("warehouseId") String warehouseId) {
        warehouseService.deleteWarehouse(warehouseId);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得仓库")
    @Parameter(name = "warehouseId", description = "仓库ID", required = true, example = "W001")
    @PreAuthorize("@ss.hasPermission('business:warehouse:query')")
    public CommonResult<WarehouseRespVO> getWarehouse(@RequestParam("warehouseId") String warehouseId) {
        WarehouseDO warehouse = warehouseService.getWarehouse(warehouseId);
        return success(BeanUtils.toBean(warehouse, WarehouseRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得仓库分页")
    @PreAuthorize("@ss.hasPermission('business:warehouse:query')")
    public CommonResult<PageResult<WarehouseRespVO>> getWarehousePage(@Valid WarehousePageReqVO pageReqVO) {
        PageResult<WarehouseDO> pageResult = warehouseService.getWarehousePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, WarehouseRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得仓库简单列表")
    @PreAuthorize("@ss.hasPermission('business:warehouse:query')")
    public CommonResult<List<WarehouseSimpleRespVO>> getWarehouseSimpleList() {
        return success(warehouseService.getWarehouseSimpleList());
    }

    @GetMapping("/export")
    @Operation(summary = "导出仓库 Excel")
    @PreAuthorize("@ss.hasPermission('business:warehouse:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWarehouseExcel(@Valid WarehousePageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<WarehouseDO> list = warehouseService.getWarehousePage(pageReqVO).getList();
        ExcelUtils.write(response, "仓库.xls", "数据", WarehouseRespVO.class, BeanUtils.toBean(list, WarehouseRespVO.class));
    }

}
