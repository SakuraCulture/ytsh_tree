package cn.iocoder.yudao.module.promotion.controller.app.coupon;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.object.ObjectUtils;
import cn.iocoder.yudao.module.product.api.spu.ProductSpuApi;
import cn.iocoder.yudao.module.product.api.spu.dto.ProductSpuRespDTO;
import cn.iocoder.yudao.module.promotion.controller.app.coupon.vo.template.AppCouponTemplatePageReqVO;
import cn.iocoder.yudao.module.promotion.controller.app.coupon.vo.template.AppCouponTemplateRespVO;
import cn.iocoder.yudao.module.promotion.convert.coupon.CouponTemplateConvert;
import cn.iocoder.yudao.module.promotion.dal.dataobject.coupon.CouponTemplateDO;
import cn.iocoder.yudao.module.promotion.enums.common.PromotionProductScopeEnum;
import cn.iocoder.yudao.module.promotion.enums.coupon.CouponTakeTypeEnum;
import cn.iocoder.yudao.module.promotion.service.coupon.CouponService;
import cn.iocoder.yudao.module.promotion.service.coupon.CouponTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;
import static java.util.Collections.singletonList;

@Tag(name = "鐢ㄦ埛 App - 浼樻儬鍔垫ā鏉?)
@RestController
@RequestMapping("/promotion/coupon-template")
@Validated
public class AppCouponTemplateController {

    @Resource
    private CouponTemplateService couponTemplateService;
    @Resource
    private CouponService couponService;

    @Resource
    private ProductSpuApi productSpuApi;

    @GetMapping("/get")
    @Operation(summary = "鑾峰緱浼樻儬鍔垫ā鐗?)
    @Parameter(name = "id", description = "浼樻儬鍒告ā鏉跨紪鍙?, required = true, example = "1024")
    public CommonResult<AppCouponTemplateRespVO> getCouponTemplate(Long id) {
        CouponTemplateDO template = couponTemplateService.getCouponTemplate(id);
        if (template == null) {
            return success(null);
        }
        // 澶勭悊鏄惁鍙鍙?        Map<Long, Boolean> canCanTakeMap = couponService.getUserCanCanTakeMap(getLoginUserId(), singletonList(template));
        return success(BeanUtils.toBean(template, AppCouponTemplateRespVO.class)
                .setCanTake(canCanTakeMap.get(template.getId())));
    }

    @GetMapping("/list")
    @Operation(summary = "鑾峰緱浼樻儬鍔垫ā鐗堝垪琛?)
    @Parameters({
            @Parameter(name = "spuId", description = "鍟嗗搧 SPU 缂栧彿"), // 鐩墠涓昏缁欏晢鍝佽鎯呬娇鐢?            @Parameter(name = "productScope", description = "浣跨敤绫诲瀷"),
            @Parameter(name = "count", description = "鏁伴噺", required = true)
    })
    public CommonResult<List<AppCouponTemplateRespVO>> getCouponTemplateList(
            @RequestParam(value = "spuId", required = false) Long spuId,
            @RequestParam(value = "productScope", required = false) Integer productScope,
            @RequestParam(value = "count", required = false, defaultValue = "10") Integer count) {
        // 1.1 澶勭悊鏌ヨ鏉′欢锛氬晢鍝佽寖鍥寸紪鍙?        Long productScopeValue = getProductScopeValue(productScope, spuId);
        // 1.2 澶勭悊鏌ヨ鏉′欢锛氶鍙栨柟寮?= 鐩存帴棰嗗彇
        List<Integer> canTakeTypes = singletonList(CouponTakeTypeEnum.USER.getType());

        // 2. 鏌ヨ
        List<CouponTemplateDO> list = couponTemplateService.getCouponTemplateList(canTakeTypes, productScope,
                productScopeValue, count);

        // 3.1 棰嗗彇鏁伴噺
        Map<Long, Boolean> canCanTakeMap = couponService.getUserCanCanTakeMap(getLoginUserId(), list);
        // 3.2 鎷兼帴杩斿洖
        return success(CouponTemplateConvert.INSTANCE.convertAppList(list, canCanTakeMap));
    }

    @GetMapping("/list-by-ids")
    @Operation(summary = "鑾峰緱浼樻儬鍔垫ā鐗堝垪琛?)
    @Parameter(name = "ids", description = "浼樻儬鍒告ā鏉跨紪鍙峰垪琛?)
    public CommonResult<List<AppCouponTemplateRespVO>> getCouponTemplateList(
            @RequestParam(value = "ids", required = false) Set<Long> ids) {
        // 1. 鏌ヨ
        List<CouponTemplateDO> list = couponTemplateService.getCouponTemplateList(ids);

        // 2.1 棰嗗彇鏁伴噺
        Map<Long, Boolean> canCanTakeMap = couponService.getUserCanCanTakeMap(getLoginUserId(), list);
        // 2.2 鎷兼帴杩斿洖
        return success(CouponTemplateConvert.INSTANCE.convertAppList(list, canCanTakeMap));
    }

    @GetMapping("/page")
    @Operation(summary = "鑾峰緱浼樻儬鍔垫ā鐗堝垎椤?)
    public CommonResult<PageResult<AppCouponTemplateRespVO>> getCouponTemplatePage(AppCouponTemplatePageReqVO pageReqVO) {
        // 1.1 澶勭悊鏌ヨ鏉′欢锛氬晢鍝佽寖鍥寸紪鍙?        Long productScopeValue = getProductScopeValue(pageReqVO.getProductScope(), pageReqVO.getSpuId());
        // 1.2 澶勭悊鏌ヨ鏉′欢锛氶鍙栨柟寮?= 鐩存帴棰嗗彇
        List<Integer> canTakeTypes = singletonList(CouponTakeTypeEnum.USER.getType());

        // 2. 鍒嗛〉鏌ヨ
        PageResult<CouponTemplateDO> pageResult = couponTemplateService.getCouponTemplatePage(
                CouponTemplateConvert.INSTANCE.convert(pageReqVO, canTakeTypes, pageReqVO.getProductScope(), productScopeValue));

        // 3.1 棰嗗彇鏁伴噺
        Map<Long, Boolean> canCanTakeMap = couponService.getUserCanCanTakeMap(getLoginUserId(), pageResult.getList());
        // 3.2 鎷兼帴杩斿洖
        return success(CouponTemplateConvert.INSTANCE.convertAppPage(pageResult, canCanTakeMap));
    }

    /**
     * 鑾峰緱鍟嗗搧鐨勪娇鐢ㄨ寖鍥寸紪鍙?     *
     * @param productScope 鍟嗗搧鑼冨洿
     * @param spuId        鍟嗗搧 SPU 缂栧彿
     * @return 鍟嗗搧鑼冨洿缂栧彿
     */
    private Long getProductScopeValue(Integer productScope, Long spuId) {
        // 閫氱敤鍒革細娌℃湁鍟嗗搧鑼冨洿
        if (ObjectUtils.equalsAny(productScope, PromotionProductScopeEnum.ALL.getScope(), null)) {
            return null;
        }
        // 鍝佺被鍒革細鏌ヨ鍟嗗搧鐨勫搧绫荤紪鍙?        if (Objects.equals(productScope, PromotionProductScopeEnum.CATEGORY.getScope()) && spuId != null) {
            ProductSpuRespDTO spu = productSpuApi.getSpu(spuId);
            return spu != null ? spu.getCategoryId() : null;
        }
        // 鍟嗗搧鍔碉細鐩存帴杩斿洖
        return spuId;
    }

}
