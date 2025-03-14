package org.agentpower.service.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.agentpower.service.secure.SecureService;
import org.agentpower.service.secure.codec.InputDecodeHandler;
import org.agentpower.service.secure.codec.InputDecodeRequired;
import org.agentpower.service.secure.codec.OutputEncodeHandler;
import org.agentpower.service.secure.codec.OutputEncodeRequired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;

@Configuration
@EnableConfigurationProperties({
        AgentPowerBaseProperties.class
})
public class AgentPowerConfiguration implements WebMvcConfigurer {

    private final SecureService service;

    public AgentPowerConfiguration(SecureService service) {
        this.service = service;
    }

    // 允许跨域访问
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true)
                .maxAge(3600)
                .allowedHeaders("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(service)
                .addPathPatterns("/**");
    }

    @Bean
    public FilterRegistrationBean<SecureService> registryFilter() {
        FilterRegistrationBean<SecureService> registration = new FilterRegistrationBean<>();
        registration.setFilter(this.service);
        registration.addUrlPatterns("/**");
        registration.setName("SecureFilter");
        registration.setOrder(1);
        return registration;
    }
}
