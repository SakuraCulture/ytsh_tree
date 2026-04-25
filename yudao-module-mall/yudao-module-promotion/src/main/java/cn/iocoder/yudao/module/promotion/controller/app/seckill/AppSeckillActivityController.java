package cn.iocoder.yudao.module.promotion.controller.app.seckill;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils;
import cn.iocoder.yudao.module.product.api.spu.ProductSpuApi;
import cn.iocoder.yudao.module.product.api.spu.dto.ProductSpuRespDTO;
import cn.iocoder.yudao.module.promotion.controller.app.seckill.vo.activity.AppSeckillActivityDetailRespVO;
import cn.iocoder.yudao.module.promotion.controller.app.seckill.vo.activity.AppSeckillActivityNowRespVO;
import cn.iocoder.yudao.module.promotion.controller.app.seckill.vo.activity.AppSeckillActivityPageReqVO;
import cn.iocoder.yudao.module.promotion.controller.app.seckill.vo.activity.AppSeckillActivityRespVO;
import cn.iocoder.yudao.module.promotion.convert.seckill.SeckillActivityConvert;
import cn.iocoder.yudao.module.promotion.dal.dataobject.seckill.SeckillActivityDO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.seckill.SeckillConfigDO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.seckill.SeckillProductDO;
import cn.iocoder.yudao.module.promotion.service.seckill.SeckillActivityService;
import cn.iocoder.yudao.module.promotion.service.seckill.SeckillConfigService;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.context.annotation.Lazy;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.cache.CacheUtils.buildAsyncReloadingCache;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.findFirst;
import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.isBetween;

@Tag(name = "鐢ㄦ埛 App - 绉掓潃娲诲姩")
@RestController
@RequestMapping("/promotion/seckill-activity")
@Validated
public class AppSeckillActivityController {

    /**
     * {@link AppSeckillActivityNowRespVO} 缂撳瓨锛岄€氳繃瀹冨紓姝ュ埛鏂?{@link #getNowSeckillActivity()} 鎵€瑕佺殑棣栭〉鏁版嵁
     */
    private final LoadingCache<String, AppSeckillActivityNowRespVO> nowSeckillActivityCache = buildAsyncReloadingCache(Duration.ofSeconds(10L),
            new CacheLoader<String, AppSeckillActivityNowRespVO>() {

                @Override
                public AppSeckillActivityNowRespVO load(String key) {
                     return getNowSeckillActivity0();
                }

            });

    @Resource
    private SeckillActivityService activityService;
    @Resource
    @Lazy
    private SeckillConfigService configService;

    @Resource
    private ProductSpuApi spuApi;

    @GetMapping("/get-now")
    @Operation(summary = "鑾峰緱褰撳墠绉掓潃娲诲姩", description = "鑾峰彇褰撳墠姝ｅ湪杩涜鐨勬椿鍔紝鎻愪緵缁欓椤典娇鐢?)
    public CommonResult<AppSeckillActivityNowRespVO> getNowSeckillActivity() {
        return success(nowSeckillActivityCache.getUnchecked("")); // 缂撳瓨
    }

    private AppSeckillActivityNowRespVO getNowSeckillActivity0() {
        // 1. 鑾峰彇褰撳墠鏃堕棿澶勫湪鍝釜绉掓潃闃舵
        SeckillConfigDO config = configService.getCurrentSeckillConfig();
        if (config == null) { // 鏃舵涓嶅瓨鍦ㄧ洿鎺ヨ繑鍥?null
            return new AppSeckillActivityNowRespVO();
        }

        // 2.1 鏌ヨ婊¤冻褰撳墠闃舵鐨勬椿鍔?        List<SeckillActivityDO> activityList = activityService.getSeckillActivityListByConfigIdAndStatus(config.getId(), CommonStatusEnum.ENABLE.getStatus());
        List<SeckillProductDO> productList = activityService.getSeckillProductListByActivityIds(
                convertList(activityList, SeckillActivityDO::getId));
        // 2.2 鑾峰彇 spu 淇℃伅
        List<ProductSpuRespDTO> spuList = spuApi.getSpuList(convertList(activityList, SeckillActivityDO::getSpuId));
        return SeckillActivityConvert.INSTANCE.convert(config, activityList, productList, spuList);
    }

    @GetMapping("/page")
    @Operation(summary = "鑾峰緱绉掓潃娲诲姩鍒嗛〉")
    public CommonResult<PageResult<AppSeckillActivityRespVO>> getSeckillActivityPage(AppSeckillActivityPageReqVO pageReqVO) {
        // 1. 鏌ヨ婊¤冻褰撳墠闃舵鐨勬椿鍔?        PageResult<SeckillActivityDO> pageResult = activityService.getSeckillActivityAppPageByConfigId(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty(pageResult.getTotal()));
        }
        List<SeckillProductDO> productList = activityService.getSeckillProductListByActivityIds(
                convertList(pageResult.getList(), SeckillActivityDO::getId));

        // 2. 鎷兼帴鏁版嵁
        List<ProductSpuRespDTO> spuList = spuApi.getSpuList(convertList(pageResult.getList(), SeckillActivityDO::getSpuId));
        return success(SeckillActivityConvert.INSTANCE.convertPage02(pageResult, productList, spuList));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "鑾峰緱绉掓潃娲诲姩鏄庣粏")
    @Parameter(name = "id", description = "娲诲姩缂栧彿", required = true, example = "1024")
    public CommonResult<AppSeckillActivityDetailRespVO> getSeckillActivity(@RequestParam("id") Long id) {
        // 1. 鑾峰彇娲诲姩
        SeckillActivityDO activity = activityService.getSeckillActivity(id);
        if (activity == null
                || ObjectUtil.equal(activity.getStatus(), CommonStatusEnum.DISABLE.getStatus())) {
            return success(null);
        }

        // 2. 鑾峰彇鏃堕棿娈?        List<SeckillConfigDO> configs = configService.getSeckillConfigListByStatus(CommonStatusEnum.ENABLE.getStatus());
        configs.removeIf(config -> !CollUtil.contains(activity.getConfigIds(), config.getId()));
        // 2.1 浼樺厛浣跨敤褰撳墠鏃堕棿娈?        SeckillConfigDO config = findFirst(configs, config0 -> isBetween(config0.getStartTime(), config0.getEndTime()));
        // 2.2 濡傛灉娌℃湁锛屽垯鑾峰彇鏈€鍚庝竴涓紝鍥犱负鍊惧悜浼樺厛灞曠ず鈥滄湭寮€濮嬧€?> 鈥滃凡缁撴潫鈥?        if (config == null) {
            config = CollUtil.getLast(configs);
        }
        if (config == null) {
            return null;
        }
        // 3. 璁＄畻寮€濮嬫椂闂淬€佺粨鏉熸椂闂?        LocalDate nowDate;
        // 3.1 濡傛灉鍦ㄦ椿鍔ㄦ棩鏈熻寖鍥村唴锛屽垯浠ヤ粖澶╀负 nowDate
        if (LocalDateTimeUtils.isBetween(activity.getStartTime(), activity.getEndTime())) {
            nowDate = LocalDate.now();
        } else {
            // 3.2 濡傛灉涓嶅湪娲诲姩鏃堕棿鑼冨洿鍐咃紝鍒欑洿鎺ヤ互娲诲姩鐨?endTime 浣滀负 nowDate锛屽洜涓鸿繕鏄€惧悜浼樺厛灞曠ず鈥滄湭寮€濮嬧€?> 鈥滃凡缁撴潫鈥?            nowDate = activity.getEndTime().toLocalDate();
        }
        LocalDateTime startTime = LocalDateTime.of(nowDate, LocalTime.parse(config.getStartTime()));
        LocalDateTime endTime = LocalDateTime.of(nowDate, LocalTime.parse(config.getEndTime()));

        // 4. 鎷兼帴鏁版嵁
        List<SeckillProductDO> productList = activityService.getSeckillProductListByActivityId(activity.getId());
        return success(SeckillActivityConvert.INSTANCE.convert3(activity, productList, startTime, endTime));
    }

    @GetMapping("/list-by-ids")
    @Operation(summary = "鑾峰緱绉掓潃娲诲姩鍒楄〃锛屽熀浜庢椿鍔ㄧ紪鍙锋暟缁?)
    @Parameter(name = "ids", description = "娲诲姩缂栧彿鏁扮粍", required = true, example = "[1024, 1025]")
    public CommonResult<List<AppSeckillActivityRespVO>> getCombinationActivityListByIds(@RequestParam("ids") List<Long> ids) {
        // 1. 鑾峰緱寮€鍚殑娲诲姩鍒楄〃
        List<SeckillActivityDO> activityList = activityService.getSeckillActivityListByIds(ids);
        activityList.removeIf(activity -> CommonStatusEnum.isDisable(activity.getStatus()));
        if (CollUtil.isEmpty(activityList)) {
            return success(Collections.emptyList());
        }
        // 2. 鎷兼帴杩斿洖
        List<SeckillProductDO> productList = activityService.getSeckillProductListByActivityIds(
                convertList(activityList, SeckillActivityDO::getId));
        List<ProductSpuRespDTO> spuList = spuApi.getSpuList(convertList(activityList, SeckillActivityDO::getSpuId));
        return success(SeckillActivityConvert.INSTANCE.convertAppList(activityList, productList, spuList));
    }

}
