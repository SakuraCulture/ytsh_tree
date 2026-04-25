package cn.iocoder.yudao.module.promotion.controller.app.bargain;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.member.api.user.MemberUserApi;
import cn.iocoder.yudao.module.member.api.user.dto.MemberUserRespDTO;
import cn.iocoder.yudao.module.product.api.spu.ProductSpuApi;
import cn.iocoder.yudao.module.product.api.spu.dto.ProductSpuRespDTO;
import cn.iocoder.yudao.module.promotion.controller.app.bargain.vo.record.AppBargainRecordCreateReqVO;
import cn.iocoder.yudao.module.promotion.controller.app.bargain.vo.record.AppBargainRecordDetailRespVO;
import cn.iocoder.yudao.module.promotion.controller.app.bargain.vo.record.AppBargainRecordRespVO;
import cn.iocoder.yudao.module.promotion.controller.app.bargain.vo.record.AppBargainRecordSummaryRespVO;
import cn.iocoder.yudao.module.promotion.convert.bargain.BargainRecordConvert;
import cn.iocoder.yudao.module.promotion.dal.dataobject.bargain.BargainActivityDO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.bargain.BargainRecordDO;
import cn.iocoder.yudao.module.promotion.enums.bargain.BargainRecordStatusEnum;
import cn.iocoder.yudao.module.promotion.service.bargain.BargainActivityService;
import cn.iocoder.yudao.module.promotion.service.bargain.BargainHelpService;
import cn.iocoder.yudao.module.promotion.service.bargain.BargainRecordService;
import cn.iocoder.yudao.module.trade.api.order.TradeOrderApi;
import cn.iocoder.yudao.module.trade.api.order.dto.TradeOrderRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "鐢ㄦ埛 App - 鐮嶄环璁板綍")
@RestController
@RequestMapping("/promotion/bargain-record")
@Validated
public class AppBargainRecordController {

    @Resource
    private BargainHelpService bargainHelpService;
    @Resource
    private BargainRecordService bargainRecordService;
    @Resource
    private BargainActivityService bargainActivityService;

    @Resource
    private TradeOrderApi tradeOrderApi;
    @Resource
    private MemberUserApi memberUserApi;
    @Resource
    private ProductSpuApi productSpuApi;

    @GetMapping("/get-summary")
    @Operation(summary = "鑾峰緱鐮嶄环璁板綍鐨勬瑕佷俊鎭?, description = "鐢ㄤ簬灏忕▼搴忛椤?)
    public CommonResult<AppBargainRecordSummaryRespVO> getBargainRecordSummary() {
        // 鐮嶄环鎴愬姛鐨勭敤鎴锋暟閲?        Integer successUserCount = bargainRecordService.getBargainRecordUserCount(
                BargainRecordStatusEnum.SUCCESS.getStatus());
        if (successUserCount == 0) {
            return success(new AppBargainRecordSummaryRespVO().setSuccessUserCount(0)
                    .setSuccessList(Collections.emptyList()));
        }
        // 鐮嶄环鎴愬姛鐨勭敤鎴峰垪琛?        List<BargainRecordDO> successList = bargainRecordService.getBargainRecordList(
                BargainRecordStatusEnum.SUCCESS.getStatus(), 7);
        List<BargainActivityDO> activityList = bargainActivityService.getBargainActivityList(
                convertSet(successList, BargainRecordDO::getActivityId));
        Map<Long, MemberUserRespDTO> userMap = memberUserApi.getUserMap(
                convertSet(successList, BargainRecordDO::getUserId));
        // 鎷兼帴杩斿洖
        return success(BargainRecordConvert.INSTANCE.convert(successUserCount, successList, activityList, userMap));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "鑾峰緱鐮嶄环璁板綍鐨勬槑缁?)
    @Parameters({
            @Parameter(name = "id", description = "鐮嶄环璁板綍缂栧彿", example = "111"), // 鍦烘櫙涓€锛氭煡鐪嬫寚瀹氱殑鐮嶄环璁板綍
            @Parameter(name = "activityId", description = "鐮嶄环娲诲姩缂栧彿", example = "222") // 鍦烘櫙浜岋細鏌ョ湅鎸囧畾鐨勭爫浠锋椿鍔?    })
    public CommonResult<AppBargainRecordDetailRespVO> getBargainRecordDetail(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "activityId", required = false) Long activityId) {
        // 1. 鏌ヨ鐮嶄环璁板綍 + 鐮嶄环娲诲姩
        Assert.isTrue(id != null || activityId != null, "鐮嶄环璁板綍缂栧彿鍜屾椿鍔ㄧ紪鍙蜂笉鑳藉悓鏃朵负绌?);
        BargainRecordDO record = id != null ? bargainRecordService.getBargainRecord(id)
                : bargainRecordService.getLastBargainRecord(getLoginUserId(), activityId);
        if (activityId == null || record != null) {
            activityId = record.getActivityId();
        }
        // 2. 鏌ヨ鍔╁姏璁板綍
        Long userId = getLoginUserId();
        Integer helpAction = getHelpAction(userId, record, activityId);
        // 3. 濡傛灉鏄嚜宸辩殑璁㈠崟锛屽垯鏌ヨ璁㈠崟淇℃伅
        TradeOrderRespDTO order = record != null && record.getOrderId() != null && record.getUserId().equals(getLoginUserId())
                ? tradeOrderApi.getOrder(record.getOrderId()) : null;
        // TODO 缁х画鏌ヨ鍒殑瀛楁

        // 鎷兼帴杩斿洖
        return success(BargainRecordConvert.INSTANCE.convert02(record, helpAction, order));
    }

    private Integer getHelpAction(Long userId, BargainRecordDO record, Long activityId) {
        // 0.1 濡傛灉娌℃湁娲诲姩锛屾棤娉曞府鐮?        if (activityId == null) {
            return null;
        }
        // 0.2 濡傛灉鏄嚜宸辩殑鐮嶄环璁板綍锛屾棤娉曞府鐮?        if (record != null && record.getUserId().equals(userId)) {
            return null;
        }

        // 1. 鍒ゆ柇鏄惁宸茬粡鍔╁姏
        if (record != null
            && bargainHelpService.getBargainHelp(record.getId(), userId) != null) {
            return AppBargainRecordDetailRespVO.HELP_ACTION_SUCCESS;
        }
        // 2. 鍒ゆ柇鏄惁婊″姪鍔?        BargainActivityDO activity = bargainActivityService.getBargainActivity(activityId);
        if (activity != null
            && bargainHelpService.getBargainHelpCountByActivity(activityId, userId) >= activity.getBargainCount()) {
            return AppBargainRecordDetailRespVO.HELP_ACTION_FULL;
        }
        // 3. 鍏佽鍔╁姏
        return AppBargainRecordDetailRespVO.HELP_ACTION_NONE;
    }

    @GetMapping("/page")
    @Operation(summary = "鑾峰緱鐮嶄环璁板綍鐨勫垎椤?)
    public CommonResult<PageResult<AppBargainRecordRespVO>> getBargainRecordPage(PageParam pageParam) {
        PageResult<BargainRecordDO> pageResult = bargainRecordService.getBargainRecordPage(getLoginUserId(), pageParam);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty(pageResult.getTotal()));
        }

        // 鎷兼帴鏁版嵁
        List<BargainActivityDO> activityList = bargainActivityService.getBargainActivityList(
                convertSet(pageResult.getList(), BargainRecordDO::getActivityId));
        List<ProductSpuRespDTO> spuList = productSpuApi.getSpuList(
                convertSet(pageResult.getList(), BargainRecordDO::getSpuId));
        List<TradeOrderRespDTO> orderList = tradeOrderApi.getOrderList(
                convertSet(pageResult.getList(), BargainRecordDO::getOrderId));
        return success(BargainRecordConvert.INSTANCE.convertPage02(pageResult, activityList, spuList, orderList));
    }

    @PostMapping("/create")
    @Operation(summary = "鍒涘缓鐮嶄环璁板綍", description = "鍙備笌鐮嶄环娲诲姩")
    public CommonResult<Long> createBargainRecord(@RequestBody AppBargainRecordCreateReqVO reqVO) {
        Long recordId = bargainRecordService.createBargainRecord(getLoginUserId(), reqVO);
        return success(recordId);
    }

}
