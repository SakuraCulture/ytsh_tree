package cn.iocoder.yudao.module.promotion.controller.app.point;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.product.api.spu.ProductSpuApi;
import cn.iocoder.yudao.module.product.api.spu.dto.ProductSpuRespDTO;
import cn.iocoder.yudao.module.promotion.controller.admin.point.vo.activity.PointActivityPageReqVO;
import cn.iocoder.yudao.module.promotion.controller.app.point.vo.AppPointActivityDetailRespVO;
import cn.iocoder.yudao.module.promotion.controller.app.point.vo.AppPointActivityPageReqVO;
import cn.iocoder.yudao.module.promotion.controller.app.point.vo.AppPointActivityRespVO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.point.PointActivityDO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.point.PointProductDO;
import cn.iocoder.yudao.module.promotion.service.point.PointActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.framework.common.util.collection.MapUtils.findAndThen;

@Tag(name = "鐢ㄦ埛 App - 绉垎鍟嗗煄娲诲姩")
@RestController
@RequestMapping("/promotion/point-activity")
@Validated
public class AppPointActivityController {

    @Resource
    private PointActivityService pointActivityService;

    @Resource
    private ProductSpuApi productSpuApi;

    @GetMapping("/page")
    @Operation(summary = "鑾峰緱绉垎鍟嗗煄娲诲姩鍒嗛〉")
    public CommonResult<PageResult<AppPointActivityRespVO>> getPointActivityPage(AppPointActivityPageReqVO pageReqVO) {
        // 1. 鏌ヨ婊¤冻褰撳墠闃舵鐨勬椿鍔?        PageResult<PointActivityDO> pageResult = pointActivityService.getPointActivityPage(
                BeanUtils.toBean(pageReqVO, PointActivityPageReqVO.class));
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty(pageResult.getTotal()));
        }

        // 2. 鎷兼帴鏁版嵁
        List<AppPointActivityRespVO> resultList = buildAppPointActivityRespVOList(pageResult.getList());
        return success(new PageResult<>(resultList, pageResult.getTotal()));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "鑾峰緱绉垎鍟嗗煄娲诲姩鏄庣粏")
    @Parameter(name = "id", description = "娲诲姩缂栧彿", required = true, example = "1024")
    public CommonResult<AppPointActivityDetailRespVO> getPointActivity(@RequestParam("id") Long id) {
        // 1. 鑾峰彇娲诲姩
        PointActivityDO activity = pointActivityService.getPointActivity(id);
        if (activity == null
                || ObjUtil.equal(activity.getStatus(), CommonStatusEnum.DISABLE.getStatus())) {
            return success(null);
        }

        // 2. 鎷兼帴鏁版嵁
        List<PointProductDO> products = pointActivityService.getPointProductListByActivityIds(Collections.singletonList(id));
        PointProductDO minProduct = getMinObject(products, PointProductDO::getPoint);
        assert minProduct != null;
        AppPointActivityDetailRespVO respVO = BeanUtils.toBean(activity, AppPointActivityDetailRespVO.class)
                .setProducts(BeanUtils.toBean(products, AppPointActivityDetailRespVO.Product.class))
                .setPoint(minProduct.getPoint()).setPrice(minProduct.getPrice());
        return success(respVO);
    }

    @GetMapping("/list-by-ids")
    @Operation(summary = "鑾峰緱绉垎鍟嗗煄娲诲姩鍒楄〃锛屽熀浜庢椿鍔ㄧ紪鍙锋暟缁?)
    @Parameter(name = "ids", description = "娲诲姩缂栧彿鏁扮粍", required = true, example = "[1024, 1025]")
    public CommonResult<List<AppPointActivityRespVO>> getCombinationActivityListByIds(@RequestParam("ids") List<Long> ids) {
        // 1. 鑾峰緱寮€鍚殑娲诲姩鍒楄〃
        List<PointActivityDO> activityList = pointActivityService.getPointActivityListByIds(ids);
        activityList.removeIf(activity -> CommonStatusEnum.isDisable(activity.getStatus()));
        if (CollUtil.isEmpty(activityList)) {
            return success(Collections.emptyList());
        }
        // 2. 鎷兼帴杩斿洖
        List<AppPointActivityRespVO> result = buildAppPointActivityRespVOList(activityList);
        return success(result);
    }

    private List<AppPointActivityRespVO> buildAppPointActivityRespVOList(List<PointActivityDO> activityList) {
        List<PointProductDO> products = pointActivityService.getPointProductListByActivityIds(
                convertSet(activityList, PointActivityDO::getId));
        Map<Long, List<PointProductDO>> productsMap = convertMultiMap(products, PointProductDO::getActivityId);
        Map<Long, ProductSpuRespDTO> spuMap = productSpuApi.getSpuMap(
                convertSet(activityList, PointActivityDO::getSpuId));
        List<AppPointActivityRespVO> result = BeanUtils.toBean(activityList, AppPointActivityRespVO.class);
        result.forEach(activity -> {
            // 璁剧疆 product 淇℃伅
            PointProductDO minProduct = getMinObject(productsMap.get(activity.getId()), PointProductDO::getPoint);
            assert minProduct != null;
            activity.setPoint(minProduct.getPoint()).setPrice(minProduct.getPrice());
            findAndThen(spuMap, activity.getSpuId(),
                    spu -> activity.setSpuName(spu.getName()).setPicUrl(spu.getPicUrl()).setMarketPrice(spu.getMarketPrice()));
        });
        return result;
    }

}
