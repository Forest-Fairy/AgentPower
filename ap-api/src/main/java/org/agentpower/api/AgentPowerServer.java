package org.agentpower.api;

import java.util.List;

public interface AgentPowerServer {

    int sendCallResult(String requestId, String callResult);

    int sendFunctionList(String requestId, List<AgentPowerFunction> functions);

    int sendFunction(String requestId, AgentPowerFunction function);

}
