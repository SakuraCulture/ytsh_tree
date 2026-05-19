package cn.iocoder.yudao.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SuppressWarnings("SpringComponentScan")
@EnableScheduling
@EnableKafka
@SpringBootApplication(scanBasePackages = {"${yudao.info.base-package}.server", "${yudao.info.base-package}.module"})
public class YudaoServerApplication /* implements ApplicationRunner */ {

    private static final Logger log = LoggerFactory.getLogger(YudaoServerApplication.class);

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        SpringApplication app = new SpringApplication(YudaoServerApplication.class);
        Environment env = app.run(args).getEnvironment();

        String version = env.getProperty("yudao.info.version", "unknown");
        log.info("🚀 应用启动成功，版本: {}", version);

        long startDuration = System.currentTimeMillis() - startTime;
        log.info("⭐ Spring Boot 启动耗时: {} ms ({} s)", startDuration, startDuration / 1000);
        log.info(
                "██                      ██                ██                                                             ████          ██  ██         ");
        log.info(
                "░██                     ░██               ░██                                                            ░██░          ░██ ░██  ██   ██ ");
        log.info(
                "██  ██████  ██████   ██████ ██████  █████      ░██    ██████ ██   ██  █████   █████   █████   ██████  ██████ ██████ ██   ██ ░██ ░██ ░░██ ██ ");
        log.info(
                "██ ██░░░░ ░░░██░░  ░░██░░█░░░██░  ██░░░██  ██████   ██░░░░ ░██  ░██ ██░░░██ ██░░░██ ██░░░██ ██░░░░  ██░░░░ ░░░██░ ░██  ░██ ░██ ░██  ░░███  ");
        log.info(
                "░██░░█████  ░██    ███████  ░██  ░███████ ██░░░██  ░░█████ ░██  ░██░██  ░░ ░██  ░░ ░███████░░█████ ░░█████   ░██  ░██  ░██ ░██ ░██   ░██   ");
        log.info(
                "░██ ░░░░░██ ░██   ██░░░░██  ░██  ░██░░░░ ░██  ░██   ░░░░░██░██  ░██░██   ██░██   ██░██░░░░  ░░░░░██ ░░░░░██  ░██  ░██  ░██ ░██ ░██   ██    ");
        log.info(
                "██  ██████   ░░██ ░░████████░███     ░░█████░░██████   ██████ ░░██████░░█████ ░░█████ ░░██████ ██████  ██████   ░██  ░░██████ ███ ███  ██     ");
        log.info(
                "░░   ░░░░░     ░░   ░░░░░░░░ ░░░       ░░░░░  ░░░░░   ░░░░░░   ░░░░░░  ░░░░░   ░░░░░   ░░░░░░ ░░░░░  ░░░░░    ░░    ░░░░░░ ░░░ ░░░  ░░");

    }
}