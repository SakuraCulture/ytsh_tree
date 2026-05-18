package cn.iocoder.yudao.module.business.controller.admin.store;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductTagBatchRespVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductTagBatchSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductTagRespVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductTagSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductTagSimpleRespVO;
import cn.iocoder.yudao.module.business.service.store.StoreProductTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 门店商品标签")
@RestController
@RequestMapping("/business/store-product-tag")
@Validated
public class StoreProductTagController {

    @Resource
    private StoreProductTagService storeProductTagService;

    @GetMapping("/list")
    @Operation(summary = "获得门店商品标签列表")
    @Parameter(name = "storeProductId", description = "门店商品 ID", required = true, example = "SP-001")
    @PreAuthorize("@ss.hasPermission('business:store-product:query')")
    public CommonResult<List<StoreProductTagRespVO>> getTagList(@RequestParam("storeProductId") String storeProductId) {
        return success(storeProductTagService.getTagList(storeProductId));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "批量获得门店商品简要标签列表")
    @Parameter(name = "storeProductIds", description = "门店商品 ID 列表", required = true)
    @PreAuthorize("@ss.hasPermission('business:store-product:query')")
    public CommonResult<List<StoreProductTagSimpleRespVO>> getSimpleTagList(@RequestParam("storeProductIds") List<String> storeProductIds) {
        return success(storeProductTagService.getSimpleTagList(storeProductIds));
    }

    @PostMapping("/save-manual")
    @Operation(summary = "保存门店商品手动标签")
    @PreAuthorize("@ss.hasPermission('business:store-product:update')")
    public CommonResult<Boolean> saveManualTags(@Valid @RequestBody StoreProductTagSaveReqVO reqVO) {
        storeProductTagService.saveManualTags(reqVO);
        return success(true);
    }

    @PostMapping("/save-manual-batch")
    @Operation(summary = "批量保存门店商品手动标签")
    @PreAuthorize("@ss.hasPermission('business:store-product:update')")
    public CommonResult<StoreProductTagBatchRespVO> saveManualTagsBatch(@Valid @RequestBody StoreProductTagBatchSaveReqVO reqVO) {
        return success(storeProductTagService.saveManualTagsBatch(reqVO));
    }

}
