package org.agentpower.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class FunctionRequest {
    private String requestId;
    private String functionName;
    private String eventType;
    private Map<String, Object> params;

    public static class Event {
        public static final String FUNC_CALL = "func-call";
        public static final String GET_FUNCTION = "get-function";
        public static final String LIST_FUNCTIONS = "list-functions";
    }
}
