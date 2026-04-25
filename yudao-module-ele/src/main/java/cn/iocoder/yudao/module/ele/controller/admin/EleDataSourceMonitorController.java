package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin-api/ele/datasource")
public class EleDataSourceMonitorController {

    private final JdbcTemplate jdbcTemplate;

    public EleDataSourceMonitorController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/pool-status")
    public CommonResult<Map<String, Object>> getPoolStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            Integer activeCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE DB = 'ytsh_dev' AND COMMAND != 'Sleep'",
                    Integer.class);

            Integer totalCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE DB = 'ytsh_dev'",
                    Integer.class);

            Integer idleCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE DB = 'ytsh_dev' AND COMMAND = 'Sleep'",
                    Integer.class);

            status.put("activeConnections", activeCount != null ? activeCount : 0);
            status.put("idleConnections", idleCount != null ? idleCount : 0);
            status.put("totalConnections", totalCount != null ? totalCount : 0);
            status.put("timestamp", System.currentTimeMillis());
            status.put("maxActive", 30);
            status.put("usagePercent", totalCount != null ? String.format("%.1f%%", (totalCount / 30.0) * 100) : "0%");

            return CommonResult.success(status);
        } catch (Exception e) {
            log.error("获取连接池状态失败", e);
            return CommonResult.error(500, "获取连接池状态失败: " + e.getMessage());
        }
    }

    @GetMapping("/connection-details")
    public CommonResult<Map<String, Object>> getConnectionDetails() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Map<String, Object>> activeConnections = jdbcTemplate.queryForList(
                    "SELECT ID, USER, HOST, COMMAND, TIME, STATE, INFO " +
                            "FROM information_schema.PROCESSLIST " +
                            "WHERE DB = 'ytsh_dev' AND COMMAND != 'Sleep' " +
                            "ORDER BY TIME DESC " +
                            "LIMIT 20");

            List<Map<String, Object>> idleConnections = jdbcTemplate.queryForList(
                    "SELECT ID, USER, HOST, COMMAND, TIME " +
                            "FROM information_schema.PROCESSLIST " +
                            "WHERE DB = 'ytsh_dev' AND COMMAND = 'Sleep' " +
                            "ORDER BY TIME DESC " +
                            "LIMIT 20");

            result.put("activeConnections", activeConnections);
            result.put("idleConnections", idleConnections);
            result.put("timestamp", System.currentTimeMillis());

            return CommonResult.success(result);
        } catch (Exception e) {
            log.error("获取连接详情失败", e);
            return CommonResult.error(500, "获取连接详情失败: " + e.getMessage());
        }
    }

    @GetMapping("/long-running-connections")
    public CommonResult<List<Map<String, Object>>> getLongRunningConnections() {
        try {
            List<Map<String, Object>> longRunningConnections = jdbcTemplate.queryForList(
                    "SELECT ID, USER, HOST, COMMAND, TIME, STATE, INFO " +
                            "FROM information_schema.PROCESSLIST " +
                            "WHERE DB = 'ytsh_dev' " +
                            "ORDER BY TIME DESC " +
                            "LIMIT 50");

            return CommonResult.success(longRunningConnections);
        } catch (Exception e) {
            log.error("获取长连接失败", e);
            return CommonResult.error(500, "获取长连接失败: " + e.getMessage());
        }
    }
}
