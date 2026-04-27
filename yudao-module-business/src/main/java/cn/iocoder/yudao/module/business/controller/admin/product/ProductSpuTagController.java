package cn.iocoder.yudao.module.business.controller.admin.product;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.ProductSpuTagRespVO;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.ProductSpuTagSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.ProductSpuTagSimpleRespVO;
import cn.iocoder.yudao.module.business.service.product.ProductSpuTagService;
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

@Tag(name = "管理后台 - 商品 SPU 标签")
@RestController
@RequestMapping("/business/product-spu-tag")
@Validated
public class ProductSpuTagController {

    @Resource
    private ProductSpuTagService productSpuTagService;

    @GetMapping("/list")
    @Operation(summary = "获得商品 SPU 标签列表")
    @Parameter(name = "productSpuId", description = "商品 SPU ID", required = true, example = "1001")
    @PreAuthorize("@ss.hasPermission('business:spu-table:query')")
    public CommonResult<List<ProductSpuTagRespVO>> getTagList(@RequestParam("productSpuId") Long productSpuId) {
        return success(productSpuTagService.getTagList(productSpuId));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "批量获得商品 SPU 简要标签列表")
    @Parameter(name = "productSpuIds", description = "商品 SPU ID 列表", required = true)
    @PreAuthorize("@ss.hasPermission('business:spu-table:query')")
    public CommonResult<List<ProductSpuTagSimpleRespVO>> getSimpleTagList(@RequestParam("productSpuIds") List<Long> productSpuIds) {
        return success(productSpuTagService.getSimpleTagList(productSpuIds));
    }

    @PostMapping("/save-manual")
    @Operation(summary = "保存商品 SPU 手动标签")
    @PreAuthorize("@ss.hasPermission('business:spu-table:update')")
    public CommonResult<Boolean> saveManualTags(@Valid @RequestBody ProductSpuTagSaveReqVO reqVO) {
        productSpuTagService.saveManualTags(reqVO);
        return success(true);
    }

}
