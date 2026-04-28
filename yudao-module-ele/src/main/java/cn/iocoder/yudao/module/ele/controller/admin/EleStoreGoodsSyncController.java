package cn.iocoder.yudao.module.ele.controller.admin;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsQueryReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsSyncLogRespVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsSyncReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsTestModeRespVO;
import cn.iocoder.yudao.module.ele.service.EleStoreGoodsSyncLogService;
import cn.iocoder.yudao.module.ele.service.EleStoreGoodsSyncService;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsPageSyncResult;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsQueryReqBO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsSyncReqBO;
import cn.iocoder.yudao.module.ele.service.dto.EleStoreGoodsQueryRespDTO;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台 - 饿了么门店商品同步")
@RestController
@RequestMapping("/ele/store-goods")
@Validated
@TenantIgnore
public class EleStoreGoodsSyncController {

    private static final String TEST_SWITCH_CONFIG_KEY = "ele.store-goods.sync.test-enabled";

    @Resource
    private EleStoreGoodsSyncService eleStoreGoodsSyncService;
    @Resource
    private EleStoreGoodsSyncLogService eleStoreGoodsSyncLogService;
    @Resource
    private ConfigApi configApi;

    @PostMapping("/sync")
    @Operation(summary = "手动触发门店商品同步")
    @PreAuthorize("@ss.hasPermission('ele:order:sync')")
    public CommonResult<Boolean> syncStoreGoods(@Valid @RequestBody EleStoreGoodsSyncReqVO reqVO) {
        EleStoreGoodsSyncReqBO reqBO = BeanUtils.toBean(reqVO, EleStoreGoodsSyncReqBO.class);
        eleStoreGoodsSyncService.syncStoreGoods(reqBO);
        return CommonResult.success(true);
    }

    @PostMapping("/query")
    @Operation(summary = "批量查询门店商品")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<EleStoreGoodsQueryRespDTO> queryStoreGoods(@Valid @RequestBody EleStoreGoodsQueryReqVO reqVO) {
        EleStoreGoodsQueryReqBO reqBO = BeanUtils.toBean(reqVO, EleStoreGoodsQueryReqBO.class);
        return CommonResult.success(eleStoreGoodsSyncService.queryStoreGoods(reqBO));
    }

    @PostMapping("/query-sync")
    @Operation(summary = "查询并同步门店商品")
    @PreAuthorize("@ss.hasPermission('ele:order:sync')")
    public CommonResult<EleStoreGoodsPageSyncResult> queryAndSyncStoreGoods(@Valid @RequestBody EleStoreGoodsQueryReqVO reqVO,
                                                                            @RequestParam(defaultValue = "false") Boolean testMode) {
        EleStoreGoodsQueryReqBO reqBO = BeanUtils.toBean(reqVO, EleStoreGoodsQueryReqBO.class);
        return CommonResult.success(eleStoreGoodsSyncService.queryAndSyncStoreGoods(reqBO, testMode));
    }

    @GetMapping("/test-mode")
    @Operation(summary = "获取商品同步测试模式开关")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<EleStoreGoodsTestModeRespVO> getTestMode() {
        String enabled = StrUtil.trim(configApi.getConfigValueByKey(TEST_SWITCH_CONFIG_KEY));
        return CommonResult.success(new EleStoreGoodsTestModeRespVO(TEST_SWITCH_CONFIG_KEY,
                "true".equalsIgnoreCase(enabled)));
    }

    @GetMapping("/sync-log/page")
    @Operation(summary = "分页查询商品同步日志")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<PageResult<EleStoreGoodsSyncLogRespVO>> getSyncLogPage(
            @Parameter(description = "平台门店编码") @RequestParam(required = false) String platformStoreId,
            @Parameter(description = "ERP 门店编码") @RequestParam(required = false) String erpStoreCode,
            @Parameter(description = "SKU 编码") @RequestParam(required = false) String skuCode,
            @Parameter(description = "是否成功") @RequestParam(required = false) Boolean success,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNo,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(eleStoreGoodsSyncLogService.getSyncLogPage(platformStoreId, erpStoreCode,
                skuCode, success, pageNo, pageSize));
    }

    @GetMapping("/sync-log/{id}")
    @Operation(summary = "获取商品同步日志详情")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<EleStoreGoodsSyncLogRespVO> getSyncLog(@PathVariable Long id) {
        return CommonResult.success(eleStoreGoodsSyncLogService.getSyncLogById(id));
    }
}
