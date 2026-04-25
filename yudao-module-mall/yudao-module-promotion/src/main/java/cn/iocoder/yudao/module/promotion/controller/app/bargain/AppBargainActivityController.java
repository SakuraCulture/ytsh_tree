package cn.iocoder.yudao.module.promotion.controller.app.bargain;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.product.api.spu.ProductSpuApi;
import cn.iocoder.yudao.module.product.api.spu.dto.ProductSpuRespDTO;
import cn.iocoder.yudao.module.promotion.controller.app.bargain.vo.activity.AppBargainActivityDetailRespVO;
import cn.iocoder.yudao.module.promotion.controller.app.bargain.vo.activity.AppBargainActivityRespVO;
import cn.iocoder.yudao.module.promotion.convert.bargain.BargainActivityConvert;
import cn.iocoder.yudao.module.promotion.dal.dataobject.bargain.BargainActivityDO;
import cn.iocoder.yudao.module.promotion.enums.bargain.BargainRecordStatusEnum;
import cn.iocoder.yudao.module.promotion.service.bargain.BargainActivityService;
import cn.iocoder.yudao.module.promotion.service.bargain.BargainRecordService;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.cache.CacheUtils.buildAsyncReloadingCache;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "鐢ㄦ埛 App - 鐮嶄环娲诲姩")
@RestController
@RequestMapping("/promotion/bargain-activity")
@Validated
public class AppBargainActivityController {

    /**
     * {@link AppBargainActivityRespVO} 缂撳瓨锛岄€氳繃瀹冨紓姝ュ埛鏂?{@link #getBargainActivityList0(Integer)} 鎵€瑕佺殑棣栭〉鏁版嵁
     */
    private final LoadingCache<Integer, List<AppBargainActivityRespVO>> bargainActivityListCache = buildAsyncReloadingCache(Duration.ofSeconds(10L),
            new CacheLoader<Integer, List<AppBargainActivityRespVO>>() {

                @Override
                public List<AppBargainActivityRespVO> load(Integer count) {
                    return getBargainActivityList0(count);
                }

            });

    @Resource
    private BargainActivityService bargainActivityService;
    @Resource
    private BargainRecordService bargainRecordService;

    @Resource
    private ProductSpuApi spuApi;

    @GetMapping("/list")
    @Operation(summary = "鑾峰緱鐮嶄环娲诲姩鍒楄〃", description = "鐢ㄤ簬灏忕▼搴忛椤?)
    @Parameter(name = "count", description = "闇€瑕佸睍绀虹殑鏁伴噺", example = "6")
    public CommonResult<List<AppBargainActivityRespVO>> getBargainActivityList(
            @RequestParam(name = "count", defaultValue = "6") Integer count) {
        return success(bargainActivityListCache.getUnchecked(count));
    }

    private List<AppBargainActivityRespVO>getBargainActivityList0(Integer count) {
        List<BargainActivityDO> list = bargainActivityService.getBargainActivityListByCount(count);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 鎷兼帴鏁版嵁
        List<ProductSpuRespDTO> spuList = spuApi.getSpuList(convertList(list, BargainActivityDO::getSpuId));
        return BargainActivityConvert.INSTANCE.convertAppList(list, spuList);
    }

    @GetMapping("/page")
    @Operation(summary = "鑾峰緱鐮嶄环娲诲姩鍒嗛〉")
    public CommonResult<PageResult<AppBargainActivityRespVO>> getBargainActivityPage(PageParam pageReqVO) {
        PageResult<BargainActivityDO> result = bargainActivityService.getBargainActivityPage(pageReqVO);
        if (CollUtil.isEmpty(result.getList())) {
            return success(PageResult.empty(result.getTotal()));
        }
        // 鎷兼帴鏁版嵁
        List<ProductSpuRespDTO> spuList = spuApi.getSpuList(convertList(result.getList(), BargainActivityDO::getSpuId));
        return success(BargainActivityConvert.INSTANCE.convertAppPage(result, spuList));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "鑾峰緱鐮嶄环娲诲姩璇︽儏")
    @Parameter(name = "id", description = "娲诲姩缂栧彿", example = "1")
    public CommonResult<AppBargainActivityDetailRespVO> getBargainActivityDetail(@RequestParam("id") Long id) {
        BargainActivityDO activity = bargainActivityService.getBargainActivity(id);
        if (activity == null) {
            return success(null);
        }
        // 鎷兼帴鏁版嵁
        Integer successUserCount = bargainRecordService.getBargainRecordUserCount(id, BargainRecordStatusEnum.SUCCESS.getStatus());
        ProductSpuRespDTO spu = spuApi.getSpu(activity.getSpuId());
        return success(BargainActivityConvert.INSTANCE.convert(activity, successUserCount, spu));
    }

}
