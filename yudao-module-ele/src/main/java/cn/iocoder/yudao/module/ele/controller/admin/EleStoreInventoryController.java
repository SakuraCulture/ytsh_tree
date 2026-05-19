package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleSkuInventoryBatchQueryReqVO;
import cn.iocoder.yudao.module.ele.service.EleSkuInventoryQueryService;
import cn.iocoder.yudao.module.ele.service.bo.EleSkuInventoryBatchQueryReqBO;
import cn.iocoder.yudao.module.ele.service.dto.EleSkuInventoryBatchQueryRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台 - 饿了么门店库存查询")
@RestController
@RequestMapping("/ele/store-inventory")
@Validated
@TenantIgnore
public class EleStoreInventoryController {

    @Resource
    private EleSkuInventoryQueryService eleSkuInventoryQueryService;

    @PostMapping("/query")
    @Operation(summary = "批量查询门店库存并治理")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<EleSkuInventoryBatchQueryRespDTO> queryStoreInventory(
            @Valid @RequestBody EleSkuInventoryBatchQueryReqVO reqVO) {
        EleSkuInventoryBatchQueryReqBO reqBO = BeanUtils.toBean(reqVO, EleSkuInventoryBatchQueryReqBO.class);
        return CommonResult.success(eleSkuInventoryQueryService.queryBatch(reqBO));
    }
}
