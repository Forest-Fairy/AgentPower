package org.agentpower.server;

import lombok.AllArgsConstructor;
import org.agentpower.api.AgentPowerFunction;
import org.agentpower.api.AgentPowerServer;
import org.agentpower.server.dto.AgentFunctionDto;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;

@Controller
@AllArgsConstructor
@RequestMapping("agent-power-server")
public class AgentPowerServerController {
    private final AgentPowerServer agentPowerServerService;
    @RequestMapping("sendCallResult")
    public int sendCallResult(
            @RequestParam String requestId,
            @RequestParam String functionName,
            @RequestParam CallResultParams params) throws IOException {
        return agentPowerServerService.sendCallResult(
                requestId, functionName, params.callResult);
    }
    public record CallResultParams(@RequestParam String callResult) {}

    @RequestMapping("sendFunctionList")
    public int sendFunctionList(@RequestParam String requestId, @RequestParam FunctionsParams params) throws IOException {
        return agentPowerServerService.sendFunctionList(requestId, params.clientServiceId, params.functions);
    }
    public record FunctionsParams(@RequestParam String clientServiceId, @RequestParam List<AgentPowerFunction.Function> functions) {}

}
