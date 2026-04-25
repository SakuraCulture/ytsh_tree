package cn.iocoder.yudao.module.ele.config;

import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 优雅关闭配置类
 *
 * 配置 Spring Boot 使用优雅关闭模式，确保应用关闭时：
 * 1. 停止接受新请求
 * 2. 等待正在执行的请求完成
 * 3. 优雅关闭所有资源
 *
 * @author 优团科技数字化团队
 */
@Configuration
public class GracefulShutdownConfiguration {

    /**
     * 配置 Web 服务器使用优雅关闭
     *
     * 当应用接收到关闭信号时：
     * 1. 停止接受新的 HTTP 请求
     * 2. 返回 503 状态给新的请求
     * 3. 等待正在处理的请求完成
     * 4. 超时后强制关闭
     */
    @Bean
    public ConfigurableServletWebServerFactory webServerFactory() {
        org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory factory = 
            new org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory();
        factory.setShutdown(Shutdown.GRACEFUL);
        return factory;
    }
}
