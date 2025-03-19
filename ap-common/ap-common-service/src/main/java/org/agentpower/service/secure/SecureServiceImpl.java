package org.agentpower.service.secure;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.agentpower.service.secure.codec.InputCodec;
import org.agentpower.service.secure.codec.InputDecodeRequired;
import org.agentpower.service.secure.codec.OutputCodec;
import org.agentpower.service.secure.codec.OutputEncodeRequired;
import org.agentpower.service.secure.recognization.*;
import org.agentpower.service.secure.xss.XssHttpServletRequestWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
// 确保 recognizer 能获取到
@AutoConfigureAfter({RecognizerConfigurations.class})
public class SecureServiceImpl implements SecureService {

    private final Recognizer recognizer;
    private final InputCodec decodeHandler;
    private final OutputCodec encodeHandler;
    private final Environment environment;

    public SecureServiceImpl(InputCodec decodeHandler, OutputCodec encodeHandler, Environment environment) {
        this.recognizer = RecognizationHelper.generateRecognizer();
        this.decodeHandler = decodeHandler;
        this.encodeHandler = encodeHandler;
        this.environment = environment;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getRequestURI().matches("/.+/favicon.ico")) {
            // 不执行
            return false;
        }
        if (handler instanceof HandlerMethod handlerMethod) {
            // 获取目标方法上的目标注解（可判断目标方法是否存在该注解）
            String requiredType = Optional.ofNullable(AnnotationUtils.findAnnotation(handlerMethod.getMethod(), AuthRequired.Type.class))
                    .map(AuthRequired.Type::value)
                    .orElseGet(() ->
                            Optional.ofNullable(AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), AuthRequired.Type.class))
                                    .map(AuthRequired.Type::value)
                                    // auth LOGIN as default
                                    .orElse(AuthRequired.Types.LOGIN_BY_DEFAULT.name())
                    );
            AuthRequired.Auth(requiredType, getLoginUser());

            InputDecodeRequired decodeRequired = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), InputDecodeRequired.class);
            if (decodeRequired == null) {
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
            final String token = request.getHeader(this.recognizer.headerField());
            if (StringUtils.isNotBlank(token)) {
                String tokenMayDecoded = Optional.ofNullable(decodeHandler.getDecoder())
                        .map(decoder -> decoder.decodeToUtf8Str(token))
                        .orElse(token);
                Optional<LoginUserVo> recognized = this.recognizer.recognize(tokenMayDecoded);
                WEB_RUNTIME_THREAD_LOCAL.set(new WebRuntime(
                        recognized.orElse(null), request, (HttpServletResponse) servletResponse));
//                request.getSession().setAttribute("qid", UUID.fastUUID().toString());
                try {
                    filterChain.doFilter(new XssHttpServletRequestWrapper(request), servletResponse);
                } finally {
                    WEB_RUNTIME_THREAD_LOCAL.remove();
                }
            }
        } else {
            // 未知直接放行
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    private static final ThreadLocal<WebRuntime> WEB_RUNTIME_THREAD_LOCAL = new ThreadLocal<>();
    @AllArgsConstructor
    @Getter
    private static class WebRuntime {
        LoginUserVo user;
        HttpServletRequest request;
        HttpServletResponse response;
    }

    public static HttpServletRequest getRequest() {
        return Optional.ofNullable(WEB_RUNTIME_THREAD_LOCAL.get())
                .map(WebRuntime::getRequest)
                .orElse(null);
    }

    public static HttpServletResponse getResponse() {
        return Optional.ofNullable(WEB_RUNTIME_THREAD_LOCAL.get())
                .map(WebRuntime::getResponse)
                .orElse(null);
    }

    public static LoginUserVo getLoginUser() {
        return Optional.ofNullable(WEB_RUNTIME_THREAD_LOCAL.get())
                .map(WebRuntime::getUser).orElse(null);
    }

    public static String getRequestId() {
        return Optional.ofNullable(WEB_RUNTIME_THREAD_LOCAL.get())
                .map(WebRuntime::getRequest)
                .map(ServletRequest::getRequestId)
                .orElse(null);
    }

    public static void sendEvent(ServerSentEvent<?> sse) throws IOException {
        HttpServletResponse response = Objects.requireNonNull(getResponse(), "获取响应体对象失败");
        SseEmitter.SseEventBuilder builder = SseEmitter.event();
        String id = sse.id();
        String event = sse.event();
        Duration retry = sse.retry();
        String comment = sse.comment();
        Object data = sse.data();
        if (id != null) {
            builder.id(id);
        }
        if (event != null) {
            builder.name(event);
        }
        if (data != null) {
            builder.data(data);
        }
        if (retry != null) {
            builder.reconnectTime(retry.toMillis());
        }
        if (comment != null) {
            builder.comment(comment);
        }
        ServerHttpResponse httpOutputMessage = new ServletServerHttpResponse(response);
        httpOutputMessage.getHeaders().set(
                HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE);
        httpOutputMessage.getHeaders().set(
                HttpHeaders.TRANSFER_ENCODING, "chunked");
        StringHttpMessageConverter converter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        Set<ResponseBodyEmitter.DataWithMediaType> dataToSend = builder.build();
        for (ResponseBodyEmitter.DataWithMediaType dataWithMediaType : dataToSend) {
            converter.write(String.valueOf(dataWithMediaType.getData()), dataWithMediaType.getMediaType(), httpOutputMessage);
        }
        httpOutputMessage.flush();
    }

}
