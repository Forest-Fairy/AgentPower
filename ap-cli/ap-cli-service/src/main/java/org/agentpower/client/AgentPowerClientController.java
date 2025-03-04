package org.agentpower.client;

import lombok.AllArgsConstructor;
import org.agentpower.api.AgentPowerFunctionDefinition;
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
    public FunctionRequest.CallResult call(String functionName, Map<String, Object> params) {
        return clientService.call(functionName, params);
    }

    @GetMapping("listFunctions")
    public List<? extends AgentPowerFunctionDefinition> listFunctions() {
        return clientService.listFunctions();
    }


}
