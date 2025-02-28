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
            @RequestParam CallResult callResult) throws IOException {
        return agentPowerServerService.sendCallResult(
                requestId, functionName, callResult.callResult);
    }
    public record CallResult(@RequestParam String callResult) {}


    @RequestMapping("sendFunction")
    public int sendFunction(@RequestParam String requestId,
                            @RequestParam Function function) throws IOException {
        return agentPowerServerService.sendFunction(requestId,
                AgentFunctionDto.builder()
                        .functionName(function.functionName())
                        .functionDesc(function.functionDesc)
                        .functionParamSchema(function.functionParamSchema)
                        .build());
    }

    @RequestMapping("sendFunctionList")
    public int sendFunctionList(@RequestParam String requestId, @RequestParam Functions functions) throws IOException {
        return agentPowerServerService.sendFunctionList(requestId,
                functions.functions.stream()
                        .map(function -> (AgentPowerFunction) AgentFunctionDto.builder()
                                .functionName(function.functionName)
                                .functionDesc(function.functionDesc)
                                .functionParamSchema(function.functionParamSchema)
                                .build()).toList());
    }

    public record Functions(@RequestParam List<Function> functions) {}
    public record Function(@RequestParam String functionName, @RequestParam String functionDesc, @RequestParam String functionParamSchema) {}

}
