package org.agentpower.server.service;


import org.agentpower.agent.tool.AgentPowerToolCallback;
import org.agentpower.api.AgentPowerFunction;
import org.agentpower.api.AgentPowerServer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentPowerServerService implements AgentPowerServer {
    @Override
    public int sendCallResult(String requestId, String functionName, String callResult) {
        return AgentPowerToolCallback.receiveCallResult(requestId, functionName, callResult);
    }

    @Override
    public int sendFunctionList(String requestId, List<AgentPowerFunction> functions) {
        // TODO send function list to client
//        return 0;
    }

    @Override
    public int sendFunction(String requestId, AgentPowerFunction function) {
        return AgentPowerToolCallback.receiveFunctionInfo(requestId, function);
    }
}
