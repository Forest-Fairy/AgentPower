package org.agentpower.server;

import lombok.AllArgsConstructor;
import org.agentpower.api.AgentPowerFunctionDefinition;
import org.agentpower.api.AgentPowerServer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
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
            @RequestParam ClientResultParams params) throws IOException {
        return agentPowerServerService.sendCallResult(
                requestId, functionName, params.content);
    }

    @RequestMapping("sendFunctionList")
    public int sendFunctionList(@RequestParam String requestId, @RequestParam ClientResultParams params) throws IOException {
        return agentPowerServerService.sendFunctionList(requestId, params.content);
    }

    public record ClientResultParams(@RequestParam String content) {}
}
