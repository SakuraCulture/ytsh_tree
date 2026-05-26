package cn.iocoder.yudao.module.ele.service;

import cn.hutool.core.io.FileUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ele.service.dto.OrderPushLogDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class OrderPushLogService {

    private static final Pattern PUSH_RECEIVE_PATTERN = Pattern.compile(
            "\\[SaaS推送接收]收到订单状态变更推送.*orderId=(\\S+?),.*status=(\\d+?),.*ticket=(\\S+?)[,\\n]"
    );

    private static final Pattern KAFKA_SEND_PATTERN = Pattern.compile(
            "\\[SaaS推送接收]成功投递Kafka.*orderId=(\\S+?),.*status=(\\d+?),"
    );

    private static final Pattern CONSUME_START_PATTERN = Pattern.compile(
            "\\[SaaS推送消费]开始处理.*orderId=(\\S+?),.*status=(\\d+?),.*ticket=(\\S+?),"
    );

    private static final Pattern CONSUME_SUCCESS_PATTERN = Pattern.compile(
            "\\[SaaS推送消费]订单状态更新成功.*orderId=(\\S+?),.*status=(\\d+?)"
    );

    private static final Pattern PUSH_WEBSOCKET_PATTERN = Pattern.compile(
            "\\[订单推送]用户(\\d+)收到订单(\\S+?)状态变更推送"
    );

    private static final Pattern CONSUME_ERROR_PATTERN = Pattern.compile(
            "\\[SaaS推送消费]消费失败.*orderId=(\\S+?),.*error=(.+?)[,\\n]"
    );

    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})"
    );

    public PageResult<OrderPushLogDTO> getPushLogs(
            String orderId,
            Integer status,
            String receiveStatus,
            Long startTime,
            Long endTime,
            Integer pageNo,
            Integer pageSize) {

        List<String> logDirs = List.of(
                System.getProperty("user.home") + "/logs",
                "/tmp/logs",
                "./logs"
        );

        List<String> logFiles = new ArrayList<>();
        for (String dir : logDirs) {
            File dirFile = new File(dir);
            if (dirFile.exists() && dirFile.isDirectory()) {
                List<File> files = FileUtil.loopFiles(dirFile, pathname ->
                        pathname.getName().contains("yudao") && pathname.getName().endsWith(".log"));
                if (files != null) {
                    for (File file : files) {
                        logFiles.add(file.getAbsolutePath());
                    }
                }
            }
        }

        if (logFiles.isEmpty()) {
            log.warn("【推送日志查询】未找到日志文件");
            PageResult<OrderPushLogDTO> emptyResult = new PageResult<>();
            emptyResult.setTotal(0L);
            emptyResult.setList(new ArrayList<>());
            return emptyResult;
        }

        List<OrderPushLogDTO> allLogs = new ArrayList<>();
        for (String logFile : logFiles) {
            try {
                List<OrderPushLogDTO> fileLogs = parseLogFile(logFile, orderId, status, receiveStatus, startTime, endTime);
                allLogs.addAll(fileLogs);
            } catch (Exception e) {
                log.warn("【推送日志查询】解析日志文件失败: {}, error={}", logFile, e.getMessage());
            }
        }

        allLogs.sort((a, b) -> Long.compare(b.getPushTime() != null ? b.getPushTime() : 0,
                a.getPushTime() != null ? a.getPushTime() : 0));

        int total = allLogs.size();
        int start = (pageNo - 1) * pageSize;
        int end = Math.min(start + pageSize, total);

        List<OrderPushLogDTO> pageLogs = start < total ? allLogs.subList(start, end) : new ArrayList<>();

        PageResult<OrderPushLogDTO> result = new PageResult<>();
        result.setTotal((long) total);
        result.setList(pageLogs);
        return result;
    }

    private List<OrderPushLogDTO> parseLogFile(
            String filePath,
            String orderId,
            Integer status,
            String receiveStatus,
            Long startTime,
            Long endTime) {

        List<OrderPushLogDTO> logs = new ArrayList<>();

        try {
            String content = FileUtil.readString(filePath, StandardCharsets.UTF_8);
            String[] lines = content.split("\n");

            OrderPushLogDTO currentLog = null;
            String currentOrderId = null;

            for (String line : lines) {
                try {
                    Matcher receiveMatcher = PUSH_RECEIVE_PATTERN.matcher(line);
                    if (receiveMatcher.find()) {
                        String oid = receiveMatcher.group(1);
                        Integer st = Integer.parseInt(receiveMatcher.group(2));
                        String ticket = receiveMatcher.group(3);

                        if (matchesFilter(oid, st, receiveStatus, startTime, endTime, orderId, status)) {
                            currentLog = new OrderPushLogDTO();
                            currentLog.setOrderId(oid);
                            currentLog.setStatus(st);
                            currentLog.setStatusName(getStatusName(st));
                            currentLog.setTicket(ticket);
                            currentLog.setReceiveStatus("RECEIVED");
                            currentLog.setConsumeStatus("PENDING");
                            currentOrderId = oid;
                        }
                    }

                    Matcher kafkaMatcher = KAFKA_SEND_PATTERN.matcher(line);
                    if (kafkaMatcher.find() && currentLog != null && kafkaMatcher.group(1).equals(currentOrderId)) {
                        currentLog.setConsumeStatus("KAFKA_SENT");
                    }

                    Matcher consumeMatcher = CONSUME_START_PATTERN.matcher(line);
                    if (consumeMatcher.find() && currentLog != null && consumeMatcher.group(1).equals(currentOrderId)) {
                        currentLog.setConsumeStatus("CONSUMING");
                        currentLog.setPushTime(extractTimestamp(line));
                        currentLog.setPushTimeStr(formatTimestamp(currentLog.getPushTime()));
                    }

                    Matcher successMatcher = CONSUME_SUCCESS_PATTERN.matcher(line);
                    if (successMatcher.find() && currentLog != null && successMatcher.group(1).equals(currentOrderId)) {
                        currentLog.setConsumeStatus("SUCCESS");
                        currentLog.setConsumeTime(extractTimestamp(line));
                        currentLog.setConsumeTimeStr(formatTimestamp(currentLog.getConsumeTime()));
                        logs.add(currentLog);
                        currentLog = null;
                        currentOrderId = null;
                    }

                    Matcher errorMatcher = CONSUME_ERROR_PATTERN.matcher(line);
                    if (errorMatcher.find() && currentLog != null && errorMatcher.group(1).equals(currentOrderId)) {
                        currentLog.setConsumeStatus("FAILED");
                        currentLog.setErrorMessage(errorMatcher.group(2));
                        logs.add(currentLog);
                        currentLog = null;
                        currentOrderId = null;
                    }

                    Matcher websocketMatcher = PUSH_WEBSOCKET_PATTERN.matcher(line);
                    if (websocketMatcher.find() && currentLog != null) {
                        currentLog.setWebsocketPushStatus("PUSHED");
                    }
                } catch (Exception e) {
                    log.debug("【推送日志查询】解析日志行失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("【推送日志查询】读取日志文件失败: {}", filePath, e);
        }

        return logs;
    }

    private boolean matchesFilter(
            String logOrderId,
            Integer logStatus,
            String receiveStatus,
            Long startTime,
            Long endTime,
            String filterOrderId,
            Integer filterStatus) {

        if (filterOrderId != null && !filterOrderId.isEmpty() && !logOrderId.contains(filterOrderId)) {
            return false;
        }
        if (filterStatus != null && !logStatus.equals(filterStatus)) {
            return false;
        }
        if (receiveStatus != null && !receiveStatus.isEmpty()) {
                    }
        if (startTime != null && endTime != null) {
            Long timestamp = System.currentTimeMillis();
            if (timestamp < startTime || timestamp > endTime) {
                return false;
            }
        }
        return true;
    }

    private String getStatusName(Integer status) {
        return switch (status) {
            case 1 -> "已支付";
            case 2 -> "已接单";
            case 3 -> "已拣货";
            case 4 -> "已打包";
            case 5 -> "已发货";
            case 6 -> "交易成功";
            case -1 -> "交易关闭";
            default -> "未知(" + status + ")";
        };
    }

    private Long extractTimestamp(String line) {
        Matcher matcher = TIMESTAMP_PATTERN.matcher(line);
        if (matcher.find()) {
            try {
                String timestampStr = matcher.group(1);
                LocalDateTime ldt = LocalDateTime.parse(
                        timestampStr,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                );
                return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (Exception e) {
                return System.currentTimeMillis();
            }
        }
        return System.currentTimeMillis();
    }

    private String formatTimestamp(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        try {
            LocalDateTime ldt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
            );
            return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return String.valueOf(timestamp);
        }
    }
}
