package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleOrderTrackingAlertVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderTrackingDO;
import cn.iocoder.yudao.module.ele.service.EleOrderTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "订单跟踪告警")
@RestController
@RequestMapping("/ele/order-tracking/alert")
@Validated
public class EleOrderTrackingAlertController {

    @Resource
    private EleOrderTrackingService eleOrderTrackingService;

    @GetMapping("/list")
    @Operation(summary = "获取未显示的告警订单列表")
    public CommonResult<List<EleOrderTrackingAlertVO>> getUnshownAlerts() {
        List<EleOrderTrackingDO> alerts = eleOrderTrackingService.getUnshownAlertOrders();
        List<EleOrderTrackingAlertVO> voList = alerts.stream().map(this::convertToVO).collect(Collectors.toList());
        return CommonResult.success(voList);
    }

    @PostMapping("/mark-shown/{id}")
    @Operation(summary = "标记告警已显示")
    public CommonResult<Boolean> markAlertAsShown(@PathVariable Long id) {
        eleOrderTrackingService.markAlertAsShown(id);
        return CommonResult.success(true);
    }

    @PostMapping("/mark-all-shown")
    @Operation(summary = "标记所有告警已显示")
    public CommonResult<Boolean> markAllAlertsAsShown() {
        List<EleOrderTrackingDO> alerts = eleOrderTrackingService.getUnshownAlertOrders();
        for (EleOrderTrackingDO alert : alerts) {
            eleOrderTrackingService.markAlertAsShown(alert.getId());
        }
        return CommonResult.success(true);
    }

    private EleOrderTrackingAlertVO convertToVO(EleOrderTrackingDO tracking) {
        EleOrderTrackingAlertVO vo = new EleOrderTrackingAlertVO();
        vo.setId(tracking.getId());
        vo.setOrderId(tracking.getOrderId());
        vo.setPlatformStoreId(tracking.getPlatformStoreId());
        vo.setErpStoreCode(tracking.getErpStoreCode());
        vo.setOrderStatus(tracking.getOrderStatus());
        vo.setAlertLevel(tracking.getAlertLevel());
        vo.setCreateTime(tracking.getOrderCreateTime());
        vo.setCreateTimeStr(formatTimestamp(tracking.getOrderCreateTime()));
        vo.setDaysElapsed(calculateDaysElapsed(tracking.getOrderCreateTime()));
        vo.setRemark(tracking.getRemark());
        return vo;
    }

    private String formatTimestamp(Long timestamp) {
        if (timestamp == null) return "-";
        try {
            LocalDateTime ldt = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(timestamp),
                    ZoneId.systemDefault()
            );
            return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return String.valueOf(timestamp);
        }
    }

    private int calculateDaysElapsed(Long createTime) {
        if (createTime == null) return 0;
        long now = System.currentTimeMillis() / 1000;
        return (int) ((now - createTime) / (24 * 60 * 60));
    }
}
