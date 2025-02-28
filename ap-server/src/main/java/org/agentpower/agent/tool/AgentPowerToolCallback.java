package org.agentpower.agent.tool;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Nonnull;
import org.agentpower.api.AgentPowerFunction;
import org.agentpower.api.FunctionRequest;
import org.agentpower.api.StatusCode;
import org.agentpower.common.RSAUtil;
import org.agentpower.configuration.client.ClientServiceConfiguration;
import org.agentpower.infrastracture.Globals;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.http.codec.ServerSentEvent;
import sun.misc.Unsafe;

import java.util.Map;
import java.util.concurrent.*;

public class AgentPowerToolCallback implements ToolCallback {

    private final String requestId;
    private final String loginUserId;
    private final ToolDefinition toolDefinition;
    private final ClientServiceConfiguration clientServiceConfiguration;

    public AgentPowerToolCallback(ClientServiceConfiguration clientServiceConfiguration, String functionName) {
        this.requestId = Globals.RequestContext.getRequestId();
        this.loginUserId = Globals.User.getLoginUser().getId();
        this.clientServiceConfiguration = clientServiceConfiguration;
        this.toolDefinition = getToolDefinitionInterval(functionName);
    }

    @Override
    public @Nonnull ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public String call(String toolInput) {
        return callClientFunction(toolInput);
    }

    private static final Map<String, Object> CALL_RESULT_CACHE =  new ConcurrentHashMap<>();
    private String callClientFunction(String toolInput) {
        Globals.Client.sendMessage(requestId, buildEvent(
                requestId, loginUserId, FunctionRequest.Event.FUNC_CALL,
                toolDefinition.name(), clientServiceConfiguration, toolInput));
        String functionDefinitionKey = wrapFunctionDefinitionKey(requestId, toolDefinition.name());
        try {
            return CompletableFuture.supplyAsync(() -> {
                while (!CALL_RESULT_CACHE.containsKey(functionDefinitionKey)) {
                    Unsafe.getUnsafe().park(true, TimeUnit.MILLISECONDS.toNanos(500));
                }
                return String.valueOf(CALL_RESULT_CACHE.remove(functionDefinitionKey));
            }).get(30, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            CALL_RESULT_CACHE.put(functionDefinitionKey, StatusCode.REQUEST_ABORT);
            throw new RuntimeException(e);
        }
    }

    public static int receiveCallResult(String requestId, String functionName, String callResult) {
        String functionDefinitionKey = wrapFunctionDefinitionKey(requestId, functionName);
        if (CALL_RESULT_CACHE.containsKey(functionDefinitionKey)) {
            return Integer.parseInt(CALL_RESULT_CACHE.get(functionDefinitionKey).toString());
        } else {
            CALL_RESULT_CACHE.put(functionDefinitionKey, callResult);
            return StatusCode.OK;
        }
    }

    private ToolDefinition getToolDefinitionInterval(String functionName) {
        // TODO 发送消息给客户端 触发客户端调用客户端服务接口
        Globals.Client.sendMessage(requestId, buildEvent(
                requestId, loginUserId, FunctionRequest.Event.GET_FUNCTION,
                functionName, clientServiceConfiguration, null));
        // TODO 接收客户端服务回调的结果
        String functionDefinitionKey = wrapFunctionDefinitionKey(requestId, functionName);
        try {
            return CompletableFuture.supplyAsync(() -> {
                while (!DEFINITION_CACHE.containsKey(functionDefinitionKey)) {
                    Unsafe.getUnsafe().park(true, TimeUnit.MILLISECONDS.toNanos(500));
                }
                return (ToolDefinition) DEFINITION_CACHE.remove(functionDefinitionKey);
            }).get(30, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            DEFINITION_CACHE.put(functionDefinitionKey, StatusCode.REQUEST_ABORT);
            throw new RuntimeException(e);
        }
    }

    private static final Map<String, Object> DEFINITION_CACHE =  new ConcurrentHashMap<>();
    public static int receiveFunctionInfo(String requestId, AgentPowerFunction agentPowerFunction) {
        String functionDefinitionKey = wrapFunctionDefinitionKey(requestId, agentPowerFunction.functionName());
        ToolDefinition definition = DefaultToolDefinition.builder()
                .name(agentPowerFunction.functionName())
                .description(agentPowerFunction.functionDesc())
                .inputSchema(agentPowerFunction.functionParamSchema())
                .build();
        if (DEFINITION_CACHE.containsKey(functionDefinitionKey)) {
            return Integer.parseInt(DEFINITION_CACHE.get(functionDefinitionKey).toString());
        } else {
            DEFINITION_CACHE.put(functionDefinitionKey, definition);
            return StatusCode.OK;
        }
    }

    private static String wrapFunctionDefinitionKey(String requestId, String functionName) {
        return requestId + "_" + functionName;
    }
    private static ServerSentEvent<?> buildEvent(
            String requestId, String loginUserId, String eventType, String functionName,
            ClientServiceConfiguration clientServiceConfiguration, String toolParams) {
        return ServerSentEvent.builder()
                .event(eventType)
                .data(JSON.toJSONString(
                        new FunctionRequest(
                                requestId,
                                functionName,
                                eventType,
                                Map.of(
                                        "configurationId", clientServiceConfiguration.getId(),
                                        "serviceUrl", clientServiceConfiguration.getServiceUrl(),
                                        "headers", clientServiceConfiguration.getHeaders(),
                                        "auth", RSAUtil.encrypt("RSA", loginUserId, clientServiceConfiguration.getServicePublicKey()),
                                        "toolParams", toolParams)
                        )))
                .build();
    }
}
