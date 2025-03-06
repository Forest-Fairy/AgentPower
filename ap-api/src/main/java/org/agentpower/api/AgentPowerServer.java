package org.agentpower.api;

import java.util.List;

public interface AgentPowerServer {

    int sendCallResult(String requestId, String functionName, String content);

    int sendFunctionList(String requestId, String content);

    record FunctionListResult(String errorInfo, List<AgentPowerFunctionDefinition> functions) {}
}
