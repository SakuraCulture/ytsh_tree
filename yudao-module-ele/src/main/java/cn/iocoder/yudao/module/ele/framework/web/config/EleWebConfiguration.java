package cn.iocoder.yudao.module.ele.framework.web.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration(proxyBeanMethods = false)
public class EleWebConfiguration {

    
    @Bean
    public GroupedOpenApi eleGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("ele");
    }

}
