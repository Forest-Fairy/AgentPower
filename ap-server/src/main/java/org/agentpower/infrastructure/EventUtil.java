//package org.agentpower.infrastracture;
//
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.http.codec.ServerSentEvent;
//import org.springframework.http.converter.StringHttpMessageConverter;
//import org.springframework.http.server.DelegatingServerHttpResponse;
//import org.springframework.http.server.ServerHttpResponse;
//import org.springframework.http.server.ServletServerHttpResponse;
//import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.time.Duration;
//import java.util.Set;
//
//public class EventUtil {
//
//    public static void sendEvent(boolean streaming, HttpServletResponse response, ServerSentEvent<?> sse) throws IOException {
//        SseEmitter.SseEventBuilder builder = SseEmitter.event();
//        String id = sse.id();
//        String event = sse.event();
//        Duration retry = sse.retry();
//        String comment = sse.comment();
//        Object data = sse.data();
//        if (id != null) {
//            builder.id(id);
//        }
//        if (event != null) {
//            builder.name(event);
//        }
//        if (data != null) {
//            builder.data(data);
//        }
//        if (retry != null) {
//            builder.reconnectTime(retry.toMillis());
//        }
//        if (comment != null) {
//            builder.comment(comment);
//        }
//
//        ServerHttpResponse httpOutputMessage = streaming
//                ? new DelegatingServerHttpResponse(new ServletServerHttpResponse(response))
//                : new ServletServerHttpResponse(response);
//        httpOutputMessage.getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);
//        httpOutputMessage.getHeaders().set(HttpHeaders.TRANSFER_ENCODING, "chunked");
//        StringHttpMessageConverter converter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
//        Set<ResponseBodyEmitter.DataWithMediaType> dataToSend = builder.build();
//        for (ResponseBodyEmitter.DataWithMediaType dataWithMediaType : dataToSend) {
//            converter.write(String.valueOf(dataWithMediaType.getData()), dataWithMediaType.getMediaType(), httpOutputMessage);
//        }
//        httpOutputMessage.flush();
//    }
//
//}
