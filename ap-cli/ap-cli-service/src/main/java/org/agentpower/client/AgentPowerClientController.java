package org.agentpower.client;

import lombok.AllArgsConstructor;
import org.agentpower.api.AgentPowerFunction;
import org.agentpower.api.FunctionRequest;
import org.agentpower.client.service.AgentPowerClientServiceImpl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("ap-client-service")
@AllArgsConstructor
public class AgentPowerClientController {
    private final AgentPowerClientServiceImpl clientService;

    @PostMapping("call")
    public String call(String requestId, String functionName, Map<String, Object> params) {
        return clientService.call(new FunctionRequest(
                requestId, functionName, FunctionRequest.Event.FUNC_CALL, params));
    }

    @GetMapping("listFunctions")
    public List<Map<String, String>> listFunctions() {
        return clientService.listFunctions().stream().map(this::functionToMap).toList();
    }

    @GetMapping("getFunction")
    public Map<String, String> getFunction(String functionName) {
        return functionToMap(clientService.getFunction(functionName));
    }

    private Map<String, String> functionToMap(AgentPowerFunction function) {
        if (function == null) {
            return null;
        }
        return Map.of(
                "functionName", function.functionName(),
                "functionDesc", function.functionDesc(),
                "functionParamSchema", function.functionParamSchema()
        );
    }

}
