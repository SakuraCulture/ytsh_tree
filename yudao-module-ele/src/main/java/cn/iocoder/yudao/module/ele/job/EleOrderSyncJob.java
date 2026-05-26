package cn.iocoder.yudao.module.ele.job;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.ele.service.EleOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.Locale;


@Slf4j
@Component
public class EleOrderSyncJob implements JobHandler {

    @Resource
    private EleOrderService eleOrderService;

    @Override
    @TenantJob
    public String execute(String param) {
        Boolean enabled = null;
        Long startTime = null;
        Long endTime = null;

        if (StrUtil.isNotBlank(param)) {
            try {
                JSONObject paramJson = JSONUtil.parseObj(param);
                enabled = paramJson.getBool("enabled", true);
                String scheduleType = paramJson.getStr("scheduleType", "time");
                LocalDate today = LocalDate.now();
                LocalTime nowTime = LocalTime.now();

                if ("dayOfMonth".equals(scheduleType)) {
                    String dayOfMonthTimeStr = paramJson.getStr("dayOfMonthTime", "00:00:00");
                    String[] timeParts = dayOfMonthTimeStr.split(":");
                    LocalTime execTime = LocalTime.of(
                            Integer.parseInt(timeParts[0]),
                            Integer.parseInt(timeParts[1]),
                            Integer.parseInt(timeParts[2])
                    );

                    if (nowTime.isAfter(execTime)) {
                        startTime = today.atTime(execTime).atZone(ZoneId.systemDefault()).toEpochSecond();
                        endTime = today.plusDays(1).atTime(execTime).atZone(ZoneId.systemDefault()).toEpochSecond();
                    } else {
                        LocalDate yesterday = today.minusDays(1);
                        startTime = yesterday.atTime(execTime).atZone(ZoneId.systemDefault()).toEpochSecond();
                        endTime = today.atTime(execTime).atZone(ZoneId.systemDefault()).toEpochSecond();
                    }
                } else if ("weekDay".equals(scheduleType)) {
                    String weekDayTimeStr = paramJson.getStr("weekDayTime", "00:00:00");
                    String[] timeParts = weekDayTimeStr.split(":");
                    LocalTime execTime = LocalTime.of(
                            Integer.parseInt(timeParts[0]),
                            Integer.parseInt(timeParts[1]),
                            Integer.parseInt(timeParts[2])
                    );

                    if (nowTime.isAfter(execTime)) {
                        startTime = today.atTime(execTime).atZone(ZoneId.systemDefault()).toEpochSecond();
                        endTime = today.plusDays(1).atTime(execTime).atZone(ZoneId.systemDefault()).toEpochSecond();
                    } else {
                        LocalDate yesterday = today.minusDays(1);
                        startTime = yesterday.atTime(execTime).atZone(ZoneId.systemDefault()).toEpochSecond();
                        endTime = today.atTime(execTime).atZone(ZoneId.systemDefault()).toEpochSecond();
                    }
                } else if ("interval".equals(scheduleType)) {
                    String intervalStartTimeStr = paramJson.getStr("intervalStartTime", "00:00:00");
                    Integer intervalHours = paramJson.getInt("intervalHours", 1);

                    String[] timeParts = intervalStartTimeStr.split(":");
                    LocalTime baseTime = LocalTime.of(
                            Integer.parseInt(timeParts[0]),
                            Integer.parseInt(timeParts[1]),
                            Integer.parseInt(timeParts[2])
                    );

                    long intervalSeconds = intervalHours * 3600L;
                    long nowEpoch = today.atTime(nowTime).atZone(ZoneId.systemDefault()).toEpochSecond();
                    long baseEpoch = today.atTime(baseTime).atZone(ZoneId.systemDefault()).toEpochSecond();

                    if (baseEpoch > nowEpoch) {
                        baseEpoch -= 86400;
                    }

                    long lastExecTime = baseEpoch + ((nowEpoch - baseEpoch) / intervalSeconds) * intervalSeconds;
                    startTime = lastExecTime;
                    endTime = nowEpoch;

                    LocalTime lastExecLocalTime = LocalTime.ofSecondOfDay(lastExecTime % 86400);
                    log.info("【按间隔同步】间隔={}小时, 上次执行={}, startTime={}, endTime={}",
                            intervalHours, lastExecLocalTime, startTime, endTime);
                } else {
                }
            } catch (Exception e) {
                log.warn("【定时任务】解析参数失败，使用默认增量同步", e);
            }
        }

        if (enabled != null && !enabled) {
            return "定时任务已禁用，跳过执行";
        }

        eleOrderService.syncAllStores(startTime, endTime);
        return "订单同步完成";
    }
}
