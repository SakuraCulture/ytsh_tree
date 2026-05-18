package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchAllOpenReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchCurrentReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchTaskPageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchTaskRespVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchTaskStorePageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryBatchTaskStoreRespVO;
import cn.iocoder.yudao.module.ele.service.EleStoreInventoryBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台 - 饿了么门店库存批量任务")
@RestController
@RequestMapping("/ele/store-inventory/batch")
@Validated
@TenantIgnore
public class EleStoreInventoryBatchController {

    @Resource
    private EleStoreInventoryBatchService inventoryBatchService;

    @PostMapping("/current")
    @Operation(summary = "创建当前门店库存批量任务")
    @PreAuthorize("@ss.hasPermission('ele:order:sync')")
    public CommonResult<Long> createCurrentStoreBatchTask(@Valid @RequestBody EleStoreInventoryBatchCurrentReqVO reqVO) {
        return CommonResult.success(inventoryBatchService.createCurrentStoreBatchTask(reqVO));
    }

    @PostMapping("/all-open")
    @Operation(summary = "创建所有开业门店库存批量任务")
    @PreAuthorize("@ss.hasPermission('ele:order:sync')")
    public CommonResult<Long> createAllOpenStoresBatchTask(@RequestBody(required = false) EleStoreInventoryBatchAllOpenReqVO reqVO) {
        return CommonResult.success(inventoryBatchService.createAllOpenStoresBatchTask(reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询库存批量任务")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<PageResult<EleStoreInventoryBatchTaskRespVO>> getTaskPage(@Valid EleStoreInventoryBatchTaskPageReqVO reqVO) {
        return CommonResult.success(inventoryBatchService.getTaskPage(reqVO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取库存批量任务详情")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<EleStoreInventoryBatchTaskRespVO> getTask(
            @Parameter(description = "任务 ID", required = true) @PathVariable Long id) {
        return CommonResult.success(inventoryBatchService.getTask(id));
    }

    @GetMapping("/{id}/stores")
    @Operation(summary = "分页查询库存批量任务门店明细")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<PageResult<EleStoreInventoryBatchTaskStoreRespVO>> getTaskStorePage(
            @Parameter(description = "任务 ID", required = true) @PathVariable Long id,
            @Valid EleStoreInventoryBatchTaskStorePageReqVO reqVO) {
        reqVO.setTaskId(id);
        return CommonResult.success(inventoryBatchService.getTaskStorePage(reqVO));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消库存批量任务")
    @PreAuthorize("@ss.hasPermission('ele:order:sync')")
    public CommonResult<Boolean> cancelTask(
            @Parameter(description = "任务 ID", required = true) @PathVariable Long id) {
        inventoryBatchService.cancelTask(id);
        return CommonResult.success(true);
    }
}
