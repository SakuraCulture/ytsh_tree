package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsGovernancePoolPageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsGovernancePoolRespVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsGovernancePoolStatusReqVO;
import cn.iocoder.yudao.module.ele.service.EleStoreGoodsGovernanceService;
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

@Tag(name = "管理后台 - 饿了么门店商品待治理池")
@RestController
@RequestMapping("/ele/store-goods/governance-pool")
@Validated
@TenantIgnore
public class EleStoreGoodsGovernancePoolController {

    @Resource
    private EleStoreGoodsGovernanceService governanceService;

    @GetMapping("/page")
    @Operation(summary = "分页查询门店商品待治理池")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<PageResult<EleStoreGoodsGovernancePoolRespVO>> getPage(
            @Valid EleStoreGoodsGovernancePoolPageReqVO reqVO) {
        return CommonResult.success(governanceService.getPage(reqVO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取门店商品待治理详情")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<EleStoreGoodsGovernancePoolRespVO> getById(
            @Parameter(description = "记录 ID", required = true) @PathVariable Long id) {
        return CommonResult.success(governanceService.getById(id));
    }

    @PutMapping("/{id}/processed")
    @Operation(summary = "标记门店商品待治理记录为已处理")
    @PreAuthorize("@ss.hasPermission('ele:order:sync')")
    public CommonResult<Boolean> markProcessed(
            @Parameter(description = "记录 ID", required = true) @PathVariable Long id,
            @RequestBody(required = false) EleStoreGoodsGovernancePoolStatusReqVO reqVO) {
        governanceService.markProcessed(id, reqVO == null ? null : reqVO.getRemark());
        return CommonResult.success(true);
    }

    @PutMapping("/{id}/ignored")
    @Operation(summary = "标记门店商品待治理记录为已忽略")
    @PreAuthorize("@ss.hasPermission('ele:order:sync')")
    public CommonResult<Boolean> markIgnored(
            @Parameter(description = "记录 ID", required = true) @PathVariable Long id,
            @RequestBody(required = false) EleStoreGoodsGovernancePoolStatusReqVO reqVO) {
        governanceService.markIgnored(id, reqVO == null ? null : reqVO.getRemark());
        return CommonResult.success(true);
    }
}
