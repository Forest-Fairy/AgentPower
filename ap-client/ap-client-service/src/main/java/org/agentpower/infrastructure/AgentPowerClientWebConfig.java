package org.agentpower.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.agentpower.service.secure.decode.InputDecodeHandler;
import org.agentpower.service.secure.decode.InputDecodeRequired;
import org.agentpower.service.secure.encode.OutputEncodeHandler;
import org.agentpower.service.secure.encode.OutputEncodeRequired;
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
@AllArgsConstructor
public class AgentPowerClientWebConfig implements HandlerInterceptor, WebMvcConfigurer {

    private final InputDecodeHandler decodeHandler;
    private final OutputEncodeHandler encodeHandler;
    private final AgentPowerClientConfigurations configuration;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        // TODO 需要校验是否可行
        if (handler instanceof HandlerMethod handlerMethod) {
            // 获取目标方法上的目标注解（可判断目标方法是否存在该注解）
            InputDecodeRequired decodeRequired = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), InputDecodeRequired.class);
            if (decodeRequired == null) {
                // 获取目标类上的目标注解（可判断目标类是否存在该注解）
                decodeRequired = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), InputDecodeRequired.class);
            }
            if (decodeRequired != null) {
                decodeHandler.decode(request, response, handlerMethod, decodeRequired, AgentPowerClientProperties.getPrivateKey());
            }
        }
        return true;
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        // TODO 需要校验是否可行
        if (handler instanceof HandlerMethod handlerMethod) {
            if (modelAndView != null) {
                Map<String, Object> model = modelAndView.getModel();
                // 获取目标方法上的目标注解（可判断目标方法是否存在该注解）
                OutputEncodeRequired encodeRequired = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), OutputEncodeRequired.class);
                if (encodeRequired == null) {
                    // 获取目标类上的目标注解（可判断目标类是否存在该注解）
                    encodeRequired = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), OutputEncodeRequired.class);
                }
                if (encodeRequired != null) {
                    encodeHandler.encode(request, response, handlerMethod, modelAndView, encodeRequired, AgentPowerClientProperties.getPublicKey());
                }
            }
        }
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
        // TODO 添加拦截器 校验请求头
        registry.addInterceptor(this);
    }
}
