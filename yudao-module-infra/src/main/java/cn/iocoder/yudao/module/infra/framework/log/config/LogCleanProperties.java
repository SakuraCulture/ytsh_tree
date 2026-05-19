package cn.iocoder.yudao.module.infra.framework.log.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 日志清理配置属性
 *
 * 通过配置文件控制各类日志的保留天数，避免硬编码。
 *
 * @author 优团科技数字化团队
 */
@Data
@Component
@ConfigurationProperties(prefix = "yudao.log")
public class LogCleanProperties {

    /**
     * API 访问日志保留天数，默认 7 天
     */
    private Integer accessLogKeepDays = 7;

    /**
     * API 错误日志保留天数，默认 7 天
     */
    private Integer errorLogKeepDays = 7;

    /**
     * 定时任务日志保留天数，默认 7 天
     */
    private Integer jobLogKeepDays = 7;

    /**
     * 操作日志保留天数，默认 7 天
     */
    private Integer operateLogKeepDays = 7;

    /**
     * 每次删除间隔的条数，防止一次删除太多造成数据库压力
     */
    private Integer deleteLimit = 100;
}
