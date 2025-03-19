package org.agentpower.api;

import lombok.Getter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

@Getter
public class FunctionRequest {
    private String requestId;
    private String eventType;
    private Map<String, Object> header;
    private Map<String, Object> body;

    public FunctionRequest(String requestId, String eventType, Header header, Body body) {
        this.requestId = requestId;
        this.eventType = eventType;
        this.header = header.content;
        this.body = body.content;
    }
    public record Header(Map<String, Object> content) {
        public static Header of(Map<String, Object> content) {
            return new Header(content);
        }
    }
    public record Body(Map<String, Object> content) {
        public static Body of(Map<String, Object> content) {
            return new Body(content);
        }
    }

    public record CallResult(String type, String content) {
        public static class Type {
            /** 出错 */
            public static final String ERROR = "ERROR";
            /** 直接返回 */
            public static final String DIRECT = "direct";
            /** 代理调用 */
            public static final String AGENT = "AGENT";
        }
    }
    public static CallResult errorCallResult(Throwable error) {
        return new CallResult(CallResult.Type.ERROR, errorTrace(error));
    }
    private static String errorTrace(Throwable error) {
        StringWriter out = new StringWriter();
        error.printStackTrace(new PrintWriter(out));
        return "%s \n %s".formatted(error.getMessage(), out.toString());
    }
}
