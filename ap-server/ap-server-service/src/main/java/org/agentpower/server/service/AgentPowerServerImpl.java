package org.agentpower.server.service;


import org.agentpower.agent.AgentChatHelper;
import org.agentpower.agent.tool.AgentPowerToolCallback;
import org.agentpower.api.AgentPowerFunctionDefinition;
import org.agentpower.api.AgentPowerServer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentPowerServerImpl implements AgentPowerServer {
    @Override
    public int sendCallResult(String requestId, String functionName, String content) {
        return AgentPowerToolCallback.receiveCallResult(requestId, functionName, content);
    }

    @Override
    public int sendFunctionList(String requestId, String content) {
        return AgentChatHelper.Prompt.receiveFunctionList(requestId, content);
    }
}
