package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsShadowMergeReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsShadowPageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsShadowRespVO;
import cn.iocoder.yudao.module.ele.service.EleStoreGoodsShadowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台 - 饿了么门店商品影子治理")
@RestController
@RequestMapping("/ele/store-goods/shadow")
@Validated
@TenantIgnore
public class EleStoreGoodsShadowController {

    @Resource
    private EleStoreGoodsShadowService shadowService;

    @GetMapping("/page")
    @Operation(summary = "分页查询影子门店品")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<PageResult<EleStoreGoodsShadowRespVO>> getPage(@Valid EleStoreGoodsShadowPageReqVO reqVO) {
        return CommonResult.success(shadowService.getShadowPage(reqVO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取影子门店品详情")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<EleStoreGoodsShadowRespVO> getById(
            @Parameter(description = "记录 ID", required = true) @PathVariable Long id) {
        return CommonResult.success(shadowService.getShadow(id));
    }

    @PutMapping("/{id}/ignored")
    @Operation(summary = "标记影子门店品为已忽略")
    @PreAuthorize("@ss.hasPermission('ele:order:sync')")
    public CommonResult<Boolean> ignore(
            @Parameter(description = "记录 ID", required = true) @PathVariable Long id) {
        shadowService.ignore(id);
        return CommonResult.success(true);
    }

    @PutMapping("/{id}/merge")
    @Operation(summary = "手动归并影子门店品")
    @PreAuthorize("@ss.hasPermission('ele:order:sync')")
    public CommonResult<Boolean> merge(
            @Parameter(description = "记录 ID", required = true) @PathVariable Long id,
            @Valid @RequestBody EleStoreGoodsShadowMergeReqVO reqVO) {
        shadowService.mergeManually(id, reqVO.getMatchedProductSkuId());
        return CommonResult.success(true);
    }
}
