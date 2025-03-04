package org.agentpower.agent.tool;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Nonnull;
import org.agentpower.agent.AgentChatHelper;
import org.agentpower.api.AgentPowerFunctionDefinition;
import org.agentpower.api.FunctionRequest;
import org.agentpower.api.StatusCode;
import org.agentpower.api.message.ChatMessageObject;
import org.agentpower.common.RSAUtil;
import org.agentpower.configuration.client.ClientServiceConfiguration;
import org.agentpower.infrastracture.Globals;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.http.codec.ServerSentEvent;
import sun.misc.Unsafe;

import java.util.Map;
import java.util.concurrent.*;

public class AgentPowerToolCallback implements ToolCallback {

    private final AgentPowerToolCallbackResolver resolver;
    private final String requestId;
    private final String loginUserId;
    private final ToolDefinition toolDefinition;
    private final ClientServiceConfiguration clientServiceConfiguration;

    public AgentPowerToolCallback(AgentPowerToolCallbackResolver resolver, ClientServiceConfiguration clientServiceConfiguration, String functionName) {
        this.resolver = resolver;
        this.requestId = Globals.RequestContext.getRequestId();
        this.loginUserId = Globals.User.getLoginUser().getId();
        this.clientServiceConfiguration = clientServiceConfiguration;
        // TODO 如果requestId 是空 导致获取不到函数定义 那就只能重新发一条消息给客户端以拉取客户端的函数信息
        AgentPowerFunctionDefinition agentPowerFunctionDefinition = AgentChatHelper.Runtime.getFunctionDefinition(requestId, clientServiceConfiguration.getId(), functionName);
        this.toolDefinition = ToolDefinition.builder()
                .name(agentPowerFunctionDefinition.functionName())
                .description(agentPowerFunctionDefinition.functionDesc())
                .inputSchema(agentPowerFunctionDefinition.functionParamSchema())
                .build();
    }

    @Override
    public @Nonnull ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public String call(String toolInput) {
        FunctionRequest.CallResult callResult = JSON.parseObject(callClientFunction(toolInput), FunctionRequest.CallResult.class);
        if (callResult.type().equals(FunctionRequest.CallResult.Type.ERROR)) {
            throw new RuntimeException(toolDefinition.name() + " 执行出错：" + callResult.content());
        }
        if (callResult.type().equals(FunctionRequest.CallResult.Type.DIRECT)) {
            return callResult.content();
        }
        if (callResult.type().equals(FunctionRequest.CallResult.Type.AGENT)) {
            // 继续调用大模型
            return resolver.callPrompt(requestId, loginUserId, clientServiceConfiguration,
                    JSON.parseObject(callResult.content(), ChatMessageObject.class));
        }
        throw new IllegalArgumentException(toolDefinition.name() + " 响应了未知结果类型：" + callResult.type() + " 结果内容为：" + callResult.content());
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
