package org.agentpower.client.service;

import lombok.AllArgsConstructor;
import org.agentpower.api.AgentPowerClientService;
import org.agentpower.api.AgentPowerFunction;
import org.agentpower.api.FunctionRequest;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class AgentPowerClientServiceImpl implements AgentPowerClientService {
    private final ApplicationContext applicationContext;
    @Override
    public String call(FunctionRequest functionRequest) {
        // TODO get function bean and handle with
        // see how spring ai calling the function bean
        return "";
    }

    @Override
    public List<AgentPowerFunction> listFunctions() {
        Map<String, AgentPowerFunction> functionMap = this.applicationContext.getBeansOfType(AgentPowerFunction.class);
        return List.copyOf(functionMap.values());
    }

//        return functionMap
//                .values()
//                .stream()
//                .map(func -> new Tuple(
//                        func,
//                        StringUtils.isBlank("") ? 1
//                                : SearchUtils.searchEach(func.functionName(), "")))
//                .filter(tuple -> ((Integer) tuple.get(1)) != -1)
//                .sorted(Comparator.comparingInt(tuple -> tuple.get(1)))
//                .map(tuple -> (AgentPowerFunction) tuple.get(0))
//                .map(func -> new Function(func.functionName(), func.functionDesc(), func.functionParamSchema()))
//                .toList();

}
