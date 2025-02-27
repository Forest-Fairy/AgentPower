package org.agentpower.api;

import java.util.List;

public interface AgentPowerServer {

    int sendCallResult(String requestId, String functionName, String callResult);

    int sendFunctionList(String requestId, List<AgentPowerFunction> functions);

}
