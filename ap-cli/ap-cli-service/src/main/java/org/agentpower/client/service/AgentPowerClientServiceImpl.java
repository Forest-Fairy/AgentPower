package org.agentpower.client.service;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.alibaba.fastjson2.JSON;
import lombok.AllArgsConstructor;
import lombok.val;
import org.agentpower.api.AgentPowerClientService;
import org.agentpower.api.AgentPowerFunctionDefinition;
import org.agentpower.api.FunctionRequest;
import org.agentpower.infrastracture.AgentPowerFunction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.resolution.SpringBeanToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class AgentPowerClientServiceImpl implements AgentPowerClientService {
    private final GenericApplicationContext applicationContext;
    private final ToolCallbackResolver toolCallbackResolver =
            SpringBeanToolCallbackResolver.builder().applicationContext(applicationContext).build();
    @Override
    public FunctionRequest.CallResult call(String functionName, Map<String, Object> params) {
        FunctionRequest.CallResult callResult;
        try {
            val fcb = Optional.ofNullable(toolCallbackResolver.resolve(functionName))
                    .orElseThrow(() -> new IllegalArgumentException("客户端服务不存在工具：" + functionName));
            val result = fcb.call(JSON.toJSONString(params));
            try {
                callResult = JSON.parseObject(result, FunctionRequest.CallResult.class);
                Optional.ofNullable(callResult.type())
                        .filter(StringUtils::isNotBlank)
                        .orElseThrow(RuntimeException::new);
            } catch (Exception ignored) {
                callResult = new FunctionRequest.CallResult(
                        FunctionRequest.CallResult.Type.DIRECT, result);
            }
        } catch (Throwable throwable) {
            callResult = new FunctionRequest.CallResult(
                    FunctionRequest.CallResult.Type.ERROR, ExceptionUtil.stacktraceToString(throwable));
        }
        return callResult;
    }

    @Override
    public List<? extends AgentPowerFunctionDefinition> listFunctions() {
        Map<String, AgentPowerFunction> functionMap = this.applicationContext.getBeansOfType(AgentPowerFunction.class);
        return functionMap.keySet().stream().map(toolCallbackResolver::resolve)
                .filter(Objects::nonNull)
                .map(func -> new AgentPowerFunctionDefinition.FunctionDefinition(
                        func.getName(), func.getDescription(), func.getInputTypeSchema()))
                .toList();
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
