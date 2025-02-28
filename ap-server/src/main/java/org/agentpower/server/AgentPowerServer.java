package org.agentpower.server;

import lombok.AllArgsConstructor;
import org.agentpower.agent.tool.AgentPowerToolCallback;
import org.agentpower.api.AgentPowerFunction;
import org.agentpower.api.FunctionRequest;
import org.agentpower.server.dto.AgentFunctionDto;
import org.agentpower.server.service.AgentPowerServerService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.List;

@Controller
@AllArgsConstructor
@RequestMapping("agent-power-server")
public class AgentPowerServer {
    private final AgentPowerServerService agentPowerServerService;

    @RequestMapping("sendCallResult")
    public int sendCallResult(FunctionRequest request) throws IOException {
        return agentPowerServerService.sendCallResult(
                request.getRequestId(), request.getFunctionName(),
                String.valueOf(request.getParams().get("callResult")));
    }

    @RequestMapping("sendFunction")
    public int sendFunction(FunctionRequest request) throws IOException {
        return agentPowerServerService.sendFunction(request.getRequestId(),
                AgentFunctionDto.builder()
                        .functionName(request.getFunctionName())
                        .functionDesc(String.valueOf(request.getParams().get("functionDesc")))
                        .functionParamSchema(String.valueOf(request.getParams().get("functionParamSchema")))
                        .build());
    }

    @RequestMapping("sendFunctionList")
    public int sendFunctionList(FunctionRequest request) throws IOException {
        // noinspection unchecked
        return agentPowerServerService.sendFunctionList(request.getRequestId(),
                (List<AgentPowerFunction>) request.getParams().get("functions"));
    }


}
