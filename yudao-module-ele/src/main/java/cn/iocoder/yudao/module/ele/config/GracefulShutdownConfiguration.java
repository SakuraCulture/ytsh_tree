package cn.iocoder.yudao.module.ele.config;

import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class GracefulShutdownConfiguration {

    
    @Bean
    public ConfigurableServletWebServerFactory webServerFactory() {
        org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory factory = 
            new org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory();
        factory.setShutdown(Shutdown.GRACEFUL);
        return factory;
    }
}
