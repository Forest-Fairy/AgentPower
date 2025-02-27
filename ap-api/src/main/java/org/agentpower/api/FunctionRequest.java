package org.agentpower.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class FunctionRequest {
    private String requestId;
    private String functionName;
    private Map<String, Object> params;
}
