package cn.iocoder.yudao.module.ele.service.traffic;

import cn.iocoder.yudao.module.ele.controller.admin.vo.EleTrafficApiRpsRespVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleTrafficHourlyStatsRespVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleTrafficTodayStatsRespVO;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class EleTrafficMonitorServiceImpl implements EleTrafficMonitorService {

    private static final Logger log = LoggerFactory.getLogger(EleTrafficMonitorServiceImpl.class);

    @Resource
    private EleTrafficMetricsCollector metricsCollector;

    @Override
    public EleTrafficTodayStatsRespVO getTodayStats() {
        return getStatsByDate(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE));
    }

    @Override
    public EleTrafficTodayStatsRespVO getStatsByDate(String dateStr) {
        EleTrafficTodayStatsRespVO vo = new EleTrafficTodayStatsRespVO();
        long requestCount = metricsCollector.getRequestCount(dateStr);
        vo.setTotalRequests(requestCount);
        vo.setTotalRequestBytes(metricsCollector.getTotalRequestBytes(dateStr));
        vo.setTotalResponseBytes(metricsCollector.getTotalResponseBytes(dateStr));
        vo.setSuccessRequests(metricsCollector.getSuccessCount(dateStr));
        vo.setFailedRequests(metricsCollector.getFailedCount(dateStr));
        vo.setMaxRequestBytes(metricsCollector.getMaxRequestBytes(dateStr));
        vo.setMaxResponseBytes(metricsCollector.getMaxResponseBytes(dateStr));

        if (requestCount > 0) {
            vo.setAvgRequestBytes(metricsCollector.getTotalRequestBytes(dateStr) / requestCount);
            vo.setAvgResponseBytes(metricsCollector.getTotalResponseBytes(dateStr) / requestCount);
            vo.setAvgDurationMs((double) metricsCollector.getTotalDurationMs(dateStr) / requestCount);
            vo.setSuccessRate((double) metricsCollector.getSuccessCount(dateStr) / requestCount * 100.0);
        } else {
            vo.setAvgRequestBytes(0L);
            vo.setAvgResponseBytes(0L);
            vo.setAvgDurationMs(0.0);
            vo.setSuccessRate(0.0);
        }

        log.debug("[流量监控] 日期统计查询: date={}, total={}, success={}, failed={}",
                dateStr, requestCount, vo.getSuccessRequests(), vo.getFailedRequests());

        return vo;
    }

    @Override
    public List<EleTrafficHourlyStatsRespVO> getHourlyStats() {
        return getHourlyStatsByDate(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE));
    }

    @Override
    public List<EleTrafficHourlyStatsRespVO> getHourlyStatsByDate(String dateStr) {
        Map<String, EleTrafficMetricsCollector.HourlyStats> hourlyStatsMap = metricsCollector.getHourlyStatsMapByDate(dateStr);

        List<EleTrafficHourlyStatsRespVO> hourlyStats = new ArrayList<>(hourlyStatsMap.size());

        for (Map.Entry<String, EleTrafficMetricsCollector.HourlyStats> entry : hourlyStatsMap.entrySet()) {
            EleTrafficMetricsCollector.HourlyStats stats = entry.getValue();

            EleTrafficHourlyStatsRespVO vo = new EleTrafficHourlyStatsRespVO();
            vo.setHour(entry.getKey());
            vo.setRequests(stats.getRequestCount());
            vo.setRequestBytes(stats.getTotalRequestBytes());
            vo.setResponseBytes(stats.getTotalResponseBytes());
            vo.setSuccessCount(stats.getSuccessCount());
            vo.setFailedCount(stats.getFailedCount());
            vo.setAvgDurationMs(stats.getAvgDurationMs());

            hourlyStats.add(vo);
        }

        hourlyStats.sort((a, b) -> a.getHour().compareTo(b.getHour()));

        log.debug("[流量监控] 小时统计查询: date={}, 共{}个小时", dateStr, hourlyStats.size());

        return hourlyStats;
    }

    @Override
    public List<String> getAvailableDates() {
        return metricsCollector.getAvailableDates();
    }

    @Override
    public EleTrafficMetricsCollector.RealtimeRecord getSingleRecord(String traceId) {
        return metricsCollector.getRecord(traceId);
    }

    @Override
    public void resetTodayStats() {
        metricsCollector.resetTodayOnly();
    }

    @Override
    public List<EleTrafficApiRpsRespVO> getApiRps() {
        List<ApiDefinition> apis = Arrays.asList(
            new ApiDefinition("ORDER_LIST", "订单列表查询", "/ele/order/list"),
            new ApiDefinition("ORDER_DETAIL", "订单详情查询", "/ele/order/detail"),
            new ApiDefinition("SYNC_ORDERS", "同步订单", "/ele/order/sync"),
            new ApiDefinition("STATUS_UPDATE", "状态更新", "/ele/order/status/update"),
            new ApiDefinition("ORDER_RETRY", "订单重试", "/ele/order/retry")
        );

        List<EleTrafficApiRpsRespVO> result = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (ApiDefinition api : apis) {
            EleTrafficApiRpsRespVO vo = new EleTrafficApiRpsRespVO();
            vo.setApiCode(api.getCode());
            vo.setApiName(api.getName());
            vo.setApiPath(api.getPath());
            vo.setCurrentRps(metricsCollector.getAvgRpsForWindow(api.getCode(), 1));
            vo.setAvgRps5s(metricsCollector.getAvgRpsForWindow(api.getCode(), 5));
            vo.setMaxRps60s(metricsCollector.getMaxRpsForWindow(api.getCode(), 60));
            vo.setTotalRequests(metricsCollector.getRequestCount());
            vo.setTimestamp(now);
            vo.setRpsHistory(metricsCollector.getRpsHistory(api.getCode(), 60));
            result.add(vo);
        }

        log.debug("[流量监控] 分接口RPS查询: 共{}个接口", result.size());
        return result;
    }

    private static class ApiDefinition {
        private final String code;
        private final String name;
        private final String path;

        public ApiDefinition(String code, String name, String path) {
            this.code = code;
            this.name = name;
            this.path = path;
        }

        public String getCode() { return code; }
        public String getName() { return name; }
        public String getPath() { return path; }
    }
}
