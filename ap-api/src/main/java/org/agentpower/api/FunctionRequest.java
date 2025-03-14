package org.agentpower.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class FunctionRequest {
    private String requestId;
    private String eventType;
    private Map<String, Object> params;

    public static class Event {
        public static final String FUNC_CALL = "func-call";
        public static final String AGENT_CALL = "agent-call";
        public static final String GET_FUNCTION = "get-function";
        public static final String LIST_FUNCTIONS = "list-functions";
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
}
