package cn.iocoder.yudao.module.promotion.controller.app.activity;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.promotion.controller.app.activity.vo.AppActivityRespVO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.bargain.BargainActivityDO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.combination.CombinationActivityDO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.seckill.SeckillActivityDO;
import cn.iocoder.yudao.module.promotion.enums.common.PromotionTypeEnum;
import cn.iocoder.yudao.module.promotion.service.bargain.BargainActivityService;
import cn.iocoder.yudao.module.promotion.service.combination.CombinationActivityService;
import cn.iocoder.yudao.module.promotion.service.seckill.SeckillActivityService;
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

import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "鐢ㄦ埛 APP - 钀ラ攢娲诲姩") // 鐢ㄤ簬鎻愪緵璺ㄥ涓椿鍔ㄧ殑 HTTP 鎺ュ彛
@RestController
@RequestMapping("/promotion/activity")
@Validated
public class AppActivityController {

    @Resource
    private CombinationActivityService combinationActivityService;
    @Resource
    private SeckillActivityService seckillActivityService;
    @Resource
    private BargainActivityService bargainActivityService;

    @GetMapping("/list-by-spu-id")
    @Operation(summary = "鑾峰緱鍗曚釜鍟嗗搧锛岃繘琛屼腑鐨勬嫾鍥€佺鏉€銆佺爫浠锋椿鍔ㄤ俊鎭?, description = "姣忕娲诲姩锛屽彧杩斿洖涓€涓?)
    @Parameter(name = "spuId", description = "鍟嗗搧缂栧彿", required = true)
    public CommonResult<List<AppActivityRespVO>> getActivityListBySpuId(@RequestParam("spuId") Long spuId) {
        List<AppActivityRespVO> activityVOList = new ArrayList<>();
        // 1. 鎷煎洟娲诲姩
        CombinationActivityDO combinationActivity = combinationActivityService.getMatchCombinationActivityBySpuId(spuId);
        if (combinationActivity != null) {
            activityVOList.add(new AppActivityRespVO(combinationActivity.getId(), PromotionTypeEnum.COMBINATION_ACTIVITY.getType(),
                    combinationActivity.getName(), combinationActivity.getSpuId(), combinationActivity.getStartTime(), combinationActivity.getEndTime()));
        }
        // 2. 绉掓潃娲诲姩
        SeckillActivityDO seckillActivity = seckillActivityService.getMatchSeckillActivityBySpuId(spuId);
        if (seckillActivity != null) {
            activityVOList.add(new AppActivityRespVO(seckillActivity.getId(), PromotionTypeEnum.SECKILL_ACTIVITY.getType(),
                    seckillActivity.getName(), seckillActivity.getSpuId(), seckillActivity.getStartTime(), seckillActivity.getEndTime()));
        }
        // 3. 鐮嶄环娲诲姩
        BargainActivityDO bargainActivity = bargainActivityService.getMatchBargainActivityBySpuId(spuId);
        if (bargainActivity != null) {
            activityVOList.add(new AppActivityRespVO(bargainActivity.getId(), PromotionTypeEnum.BARGAIN_ACTIVITY.getType(),
                    bargainActivity.getName(), bargainActivity.getSpuId(), bargainActivity.getStartTime(), bargainActivity.getEndTime()));
        }
        return success(activityVOList);
    }

}
