package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderTrackingDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleOrderTrackingMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class EleOrderTrackingService {

    @Resource
    private EleOrderTrackingMapper eleOrderTrackingMapper;

    private static final long THREE_DAYS_SECONDS = 3 * 24 * 60 * 60L;
    private static final long FIVE_DAYS_SECONDS = 5 * 24 * 60 * 60L;

    @Transactional(rollbackFor = Exception.class)
    public void startTracking(String orderId, String platformStoreId, String merchantCode,
                               String erpStoreCode, String channelOrderId, Integer orderStatus, Long orderCreateTime) {
        try {
            EleOrderTrackingDO existing = eleOrderTrackingMapper.selectByOrderId(orderId);
            if (existing != null) {
                log.info("【订单跟踪】订单已在跟踪中，orderId={}", orderId);
                return;
            }

            EleOrderTrackingDO tracking = EleOrderTrackingDO.builder()
                    .orderId(orderId)
                    .platformStoreId(platformStoreId)
                    .merchantCode(merchantCode)
                    .erpStoreCode(erpStoreCode)
                    .channelOrderId(channelOrderId)
                    .orderStatus(orderStatus)
                    .orderCreateTime(orderCreateTime)
                    .lastPushTime(System.currentTimeMillis() / 1000)
                    .lastPushStatus(orderStatus)
                    .trackingStatus("TRACKING")
                    .alertLevel(null)
                    .alertShown(0)
                    .remark("自动跟踪：订单状态未完结")
                    .build();

            eleOrderTrackingMapper.insert(tracking);
            log.info("【订单跟踪】开始跟踪订单，orderId={}, status={}, storeId={}", orderId, orderStatus, platformStoreId);
        } catch (Exception e) {
            log.error("【订单跟踪】开始跟踪失败，orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateTracking(String orderId, Integer newStatus) {
        try {
            EleOrderTrackingDO tracking = eleOrderTrackingMapper.selectByOrderId(orderId);
            if (tracking == null) {
                log.info("【订单跟踪】完结或历史订单无跟踪记录，视为无需跟踪，orderId={}, status={}", orderId, newStatus);
                return;
            }

            tracking.setOrderStatus(newStatus);
            tracking.setLastPushTime(System.currentTimeMillis() / 1000);
            tracking.setLastPushStatus(newStatus);

            if (newStatus == -1 || newStatus == 6) {
                tracking.setTrackingStatus("COMPLETED");
                tracking.setAlertLevel(null);
                tracking.setAlertShown(0);
                log.info("【订单跟踪】订单已完结，orderId={}, status={}", orderId, newStatus);
            } else {
                log.info("【订单跟踪】更新订单状态，orderId={}, status={}", orderId, newStatus);
            }

            eleOrderTrackingMapper.updateById(tracking);
        } catch (Exception e) {
            log.error("【订单跟踪】更新跟踪失败，orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    public void scanTimeoutOrders() {
        log.info("【订单跟踪】开始扫描超时订单...");
        int warningCount = 0;
        int criticalCount = 0;

        try {
            long now = System.currentTimeMillis() / 1000;
            long threeDaysAgo = now - THREE_DAYS_SECONDS;
            long fiveDaysAgo = now - FIVE_DAYS_SECONDS;

            List<EleOrderTrackingDO> trackingOrders = eleOrderTrackingMapper.selectTrackingOrders();

            for (EleOrderTrackingDO tracking : trackingOrders) {
                try {
                    Long orderCreateTime = tracking.getOrderCreateTime();
                    if (orderCreateTime == null) {
                        continue;
                    }
                    long daysElapsed = (now - orderCreateTime) / (24 * 60 * 60);

                    if (orderCreateTime < fiveDaysAgo) {
                        if (!"CRITICAL".equals(tracking.getAlertLevel())) {
                            tracking.setAlertLevel("CRITICAL");
                            tracking.setAlertShown(0);
                            tracking.setTrackingStatus("TIMEOUT");
                            tracking.setRemark("订单超过5天未完结，严重告警！");
                            eleOrderTrackingMapper.updateById(tracking);
                            criticalCount++;
                            log.warn("【订单跟踪】严重告警订单，orderId={}, 门店={}, 创建时间={}, 已过{}天",
                                    tracking.getOrderId(), tracking.getPlatformStoreId(),
                                    formatTimestamp(orderCreateTime), daysElapsed);
                        }
                    } else if (orderCreateTime < threeDaysAgo) {
                        if (!"WARNING".equals(tracking.getAlertLevel()) && !"CRITICAL".equals(tracking.getAlertLevel())) {
                            tracking.setAlertLevel("WARNING");
                            tracking.setAlertShown(0);
                            tracking.setRemark("订单超过3天未完结，警告！");
                            eleOrderTrackingMapper.updateById(tracking);
                            warningCount++;
                            log.warn("【订单跟踪】警告订单，orderId={}, 门店={}, 创建时间={}, 已过{}天",
                                    tracking.getOrderId(), tracking.getPlatformStoreId(),
                                    formatTimestamp(orderCreateTime), daysElapsed);
                        }
                    }
                } catch (Exception e) {
                    log.error("【订单跟踪】处理单个订单失败，orderId={}, error={}", tracking.getOrderId(), e.getMessage());
                }
            }

            log.info("【订单跟踪】扫描完成，新增警告{}个，严重告警{}个", warningCount, criticalCount);
        } catch (Exception e) {
            log.error("【订单跟踪】扫描超时订单失败，error={}", e.getMessage(), e);
        }
    }

    public List<EleOrderTrackingDO> getUnshownAlertOrders() {
        return eleOrderTrackingMapper.selectList(new LambdaQueryWrapperX<EleOrderTrackingDO>()
                .eq(EleOrderTrackingDO::getTrackingStatus, "TIMEOUT")
                .eq(EleOrderTrackingDO::getAlertShown, 0)
                .isNotNull(EleOrderTrackingDO::getAlertLevel)
                .orderByAsc(EleOrderTrackingDO::getOrderCreateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public void markAlertAsShown(Long id) {
        EleOrderTrackingDO tracking = eleOrderTrackingMapper.selectById(id);
        if (tracking != null) {
            tracking.setAlertShown(1);
            eleOrderTrackingMapper.updateById(tracking);
        }
    }

    private String formatTimestamp(long timestamp) {
        try {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .format(Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()));
        } catch (Exception e) {
            return String.valueOf(timestamp);
        }
    }
}
