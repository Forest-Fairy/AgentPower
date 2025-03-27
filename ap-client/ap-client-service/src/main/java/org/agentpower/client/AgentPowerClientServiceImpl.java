package org.agentpower.client;

import com.alibaba.fastjson2.JSON;
import lombok.val;
import org.agentpower.api.AgentPowerClientService;
import org.agentpower.api.AgentPowerFunctionDefinition;
import org.agentpower.api.FunctionRequest;
import org.agentpower.api.info.ClientServiceConfigurationInfo;
import org.agentpower.client.api.AgentPowerFunction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.SpringBeanToolCallbackResolver;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentPowerClientServiceImpl implements AgentPowerClientService {
    private final GenericApplicationContext applicationContext;
    private SpringBeanToolCallbackResolver toolCallbackResolver;
    private final Map<String, AgentPowerFunction> beansOfType;

    public AgentPowerClientServiceImpl(GenericApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        beansOfType = new ConcurrentHashMap<>(applicationContext.getBeansOfType(AgentPowerFunction.class));
        refreshResolver();
    }

    public boolean isFunctionObject(Object function) {
        return function instanceof AgentPowerFunction;
    }

    public boolean addTool(String toolName, Object function) {
        if (isFunctionObject(function)) {
            val func = (AgentPowerFunction) function;
            synchronized (beansOfType) {
                val old = beansOfType.put(toolName, func);
                beansOfType.put(toolName, old != null ? old : func);
                if (old != null) {
                    throw new IllegalStateException("工具已存在，请先卸载：" + toolName);
                }
                return true;
            }
        } else {
//            Objects.requireNonNull(function, "工具对象不存在，请检查代码逻辑");
            return false;
        }
    }

    public void removeTool(String toolName) {
        val old = beansOfType.remove(toolName);
        if (old == null) {
            throw new IllegalStateException("工具不存在，请先添加：" + toolName);
        }
    }

    public void refreshResolver() {
        toolCallbackResolver = SpringBeanToolCallbackResolver.builder().applicationContext(applicationContext).build();
    }

    @Override
    public FunctionRequest.CallResult call(String functionName, Map<String, Object> params) {
        val classLoader = Thread.currentThread().getContextClassLoader();
        FunctionRequest.CallResult callResult;
        try {
            // 将线程类加载器切换到创建bean的加载器，以确保bean执行过程能获取到jar包的类
            val agentPowerFunction = Optional.ofNullable(beansOfType.get(functionName))
                    .orElseThrow(() -> new IllegalArgumentException("客户端服务不存在工具：" + functionName));
            Thread.currentThread().setContextClassLoader(agentPowerFunction.getClass().getClassLoader());
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
            callResult = FunctionRequest.errorCallResult(throwable);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        return callResult;
    }

    @Override
    public List<? extends AgentPowerFunctionDefinition> listFunctions() {
        return beansOfType.keySet().stream().map(toolCallbackResolver::resolve)
                .filter(Objects::nonNull)
                .map(ToolCallback::getToolDefinition)
                .map(func -> new AgentPowerFunctionDefinition.FunctionDefinition(
                        func.name(), func.description(), func.inputSchema()))
                .toList();
    }

    public ClientServiceConfigurationInfo getClientInfo() {
        // 先返回null 后续再详细设计完善实现
        return null;
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
