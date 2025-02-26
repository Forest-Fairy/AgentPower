package org.agentpower.server;

import org.agentpower.agent.tool.AgentPowerToolCallback;
import org.agentpower.api.FunctionRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

@Controller
@RequestMapping("agent-power-server")
public class AgentPowerServer {

    @RequestMapping("receiveResult")
    public int receiveFileContent(FunctionRequest request) throws IOException {
        return AgentPowerToolCallback.receiveRequestResult(request);
    }
}
