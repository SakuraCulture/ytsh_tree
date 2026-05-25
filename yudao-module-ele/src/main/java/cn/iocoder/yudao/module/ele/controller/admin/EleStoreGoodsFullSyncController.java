package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncAllOpenReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncCurrentReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncTaskPageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncTaskRespVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncTaskStorePageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsFullSyncTaskStoreRespVO;
import cn.iocoder.yudao.module.ele.service.EleStoreGoodsFullSyncService;
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

@Tag(name = "管理后台 - 饿了么门店商品全量同步任务")
@RestController
@RequestMapping("/ele/store-goods/full-sync")
@Validated
@TenantIgnore
public class EleStoreGoodsFullSyncController {

    @Resource
    private EleStoreGoodsFullSyncService fullSyncService;

    @PostMapping("/current")
    @Operation(summary = "创建当前门店商品全量同步任务")
    @PreAuthorize("@ss.hasPermission('ele:order:sync')")
    public CommonResult<Long> createCurrentStoreFullSync(@Valid @RequestBody EleStoreGoodsFullSyncCurrentReqVO reqVO) {
        return CommonResult.success(fullSyncService.createCurrentStoreFullSync(reqVO));
    }

    @PostMapping("/all-open")
    @Operation(summary = "创建所有开业门店商品全量同步任务")
    @PreAuthorize("@ss.hasPermission('ele:order:sync')")
    public CommonResult<Boolean> createAllOpenStoresFullSync(@RequestBody(required = false) EleStoreGoodsFullSyncAllOpenReqVO reqVO) {
        return CommonResult.success(fullSyncService.createAllOpenStoresFullSync(reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询商品全量同步任务")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<PageResult<EleStoreGoodsFullSyncTaskRespVO>> getTaskPage(
            @Valid EleStoreGoodsFullSyncTaskPageReqVO reqVO) {
        return CommonResult.success(fullSyncService.getTaskPage(reqVO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取商品全量同步任务详情")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<EleStoreGoodsFullSyncTaskRespVO> getTask(
            @Parameter(description = "任务 ID", required = true) @PathVariable Long id) {
        return CommonResult.success(fullSyncService.getTask(id));
    }

    @GetMapping("/{id}/stores")
    @Operation(summary = "分页查询商品全量同步任务门店明细")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<PageResult<EleStoreGoodsFullSyncTaskStoreRespVO>> getTaskStorePage(
            @Parameter(description = "任务 ID", required = true) @PathVariable Long id,
            @Valid EleStoreGoodsFullSyncTaskStorePageReqVO reqVO) {
        reqVO.setTaskId(id);
        return CommonResult.success(fullSyncService.getTaskStorePage(reqVO));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消商品全量同步任务")
    @PreAuthorize("@ss.hasPermission('ele:order:sync')")
    public CommonResult<Boolean> cancelTask(
            @Parameter(description = "任务 ID", required = true) @PathVariable Long id) {
        fullSyncService.cancelTask(id);
        return CommonResult.success(true);
    }
}
