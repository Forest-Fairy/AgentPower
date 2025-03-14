package org.agentpower.service.secure;

import cn.hutool.core.lang.UUID;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.agentpower.service.secure.codec.InputDecodeHandler;
import org.agentpower.service.secure.codec.InputDecodeRequired;
import org.agentpower.service.secure.codec.OutputEncodeHandler;
import org.agentpower.service.secure.codec.OutputEncodeRequired;
import org.agentpower.service.secure.recognization.LoginUserVo;
import org.agentpower.service.secure.recognization.RecognizationHelper;
import org.agentpower.service.secure.recognization.Recognizer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Service
public class SecureServiceImpl implements SecureService {

    private final Recognizer recognizer;
    private final InputDecodeHandler decodeHandler;
    private final OutputEncodeHandler encodeHandler;
    private final SpringUtil springUtil;

    public SecureServiceImpl(InputDecodeHandler decodeHandler, OutputEncodeHandler encodeHandler, SpringUtil springUtil) {
        this.recognizer = RecognizationHelper.generateRecognizer();
        this.decodeHandler = decodeHandler;
        this.encodeHandler = encodeHandler;
        this.springUtil = springUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            // 获取目标方法上的目标注解（可判断目标方法是否存在该注解）
            InputDecodeRequired decodeRequired = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), InputDecodeRequired.class);
            if (decodeRequired == null) {
                // 获取目标类上的目标注解（可判断目标类是否存在该注解）
                decodeRequired = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), InputDecodeRequired.class);
            }
            if (decodeRequired != null) {
                decodeHandler.decode(request, response, handlerMethod, decodeRequired);
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
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
                    encodeHandler.encode(request, response, handlerMethod, modelAndView, encodeRequired);
                }
            }
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest request) {
            String token = request.getHeader(this.recognizer.headerField());
            if (StringUtils.isNotBlank(token)) {
                Optional<LoginUserVo> recognized = this.recognizer.recognize(token);
                if (recognized.isPresent()) {
                    request.getSession().setAttribute("requestId", UUID.fastUUID().toString());
                    request.getSession().setAttribute("user", recognized.get());
                    filterChain.doFilter(request, servletResponse);
                }
            }
        } else {
            // 未知直接放行
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }
}
