package cn.iocoder.yudao.module.promotion.controller.app.reward;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.promotion.controller.app.reward.vo.AppRewardActivityRespVO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.reward.RewardActivityDO;
import cn.iocoder.yudao.module.promotion.service.reward.RewardActivityService;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "鐢ㄦ埛 App - 婊″噺閫佹椿鍔?)
@RestController
@RequestMapping("/promotion/reward-activity")
@Validated
public class AppRewardActivityController {

    @Resource
    private RewardActivityService rewardActivityService;

    @GetMapping("/get")
    @Operation(summary = "鑾峰緱婊″噺閫佹椿鍔?)
    @Parameter(name = "id", description = "缂栧彿", required = true, example = "1024")
    public CommonResult<AppRewardActivityRespVO> getRewardActivity(@RequestParam("id") Long id) {
        RewardActivityDO activity = rewardActivityService.getRewardActivity(id);
        if (activity == null) {
            return success(null);
        }
        // 鎷兼帴 Rule 鎻忚堪
        AppRewardActivityRespVO activityVO = BeanUtils.toBean(activity, AppRewardActivityRespVO.class);
        for (int i = 0; i < activityVO.getRules().size(); i++) {
            AppRewardActivityRespVO.Rule ruleVO = activityVO.getRules().get(i);
            RewardActivityDO.Rule rule = activity.getRules().get(i);
            ruleVO.setDescription(rewardActivityService.getRewardActivityRuleDescription(activity.getConditionType(), rule));
        }
        return success(activityVO);
    }

}
