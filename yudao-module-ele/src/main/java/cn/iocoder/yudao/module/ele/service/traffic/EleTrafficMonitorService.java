package cn.iocoder.yudao.module.ele.service.traffic;

import cn.iocoder.yudao.module.ele.controller.admin.vo.EleTrafficApiRpsRespVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleTrafficHourlyStatsRespVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleTrafficTodayStatsRespVO;

import java.util.List;

public interface EleTrafficMonitorService {

    EleTrafficTodayStatsRespVO getTodayStats();

    EleTrafficTodayStatsRespVO getStatsByDate(String dateStr);

    List<EleTrafficHourlyStatsRespVO> getHourlyStats();

    List<EleTrafficHourlyStatsRespVO> getHourlyStatsByDate(String dateStr);

    EleTrafficMetricsCollector.RealtimeRecord getSingleRecord(String traceId);

    void resetTodayStats();

    List<String> getAvailableDates();

    List<EleTrafficApiRpsRespVO> getApiRps();
}
