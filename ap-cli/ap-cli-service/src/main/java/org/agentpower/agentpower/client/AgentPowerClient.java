package org.agentpower.agentpower.client;

import cn.hutool.core.lang.Tuple;
import org.agentpower.api.AgentPowerFunction;
import org.agentpower.api.FunctionRequest;
import org.agentpower.common.SearchUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("ap-client-service")
public class AgentPowerClient {
    private final ApplicationContext applicationContext;

    public AgentPowerClient(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostMapping("call")
    public String call(FunctionRequest functionRequest) {
        // TODO get function bean and handle with
        // see how spring ai calling the function bean
        return "";
    }

    public record FunctionDesc(String functionName, String functionDesc, Map<String, Object> functionParamDesc) {
    }

    @GetMapping("list")
    public List<FunctionDesc> list(String query) {
        Map<String, AgentPowerFunction> functionMap = this.applicationContext.getBeansOfType(AgentPowerFunction.class);
        return functionMap
                .values()
                .stream()
                .map(func -> new Tuple(
                        func,
                        StringUtils.isBlank(query) ? 1
                                : SearchUtils.searchEach(func.functionName(), query)))
                .filter(tuple -> ((Integer) tuple.get(1)) != -1)
                .sorted(Comparator.comparingInt(tuple -> tuple.get(1)))
                .map(tuple -> (AgentPowerFunction) tuple.get(0))
                .map(func ->
                        new FunctionDesc(
                                func.functionName(),
                                func.functionDesc(),
                                func.functionParamDesc())
                ).toList();
    }

}
