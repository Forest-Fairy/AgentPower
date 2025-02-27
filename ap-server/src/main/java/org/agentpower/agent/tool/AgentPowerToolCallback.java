package org.agentpower.agent.tool;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Nonnull;
import org.agentpower.api.FunctionRequest;
import org.agentpower.api.StatusCode;
import org.agentpower.configuration.client.ClientServiceConfiguration;
import org.agentpower.infrastracture.Globals;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.http.codec.ServerSentEvent;

import java.util.Map;

public class AgentPowerToolCallback implements ToolCallback {

    private final ToolDefinition toolDefinition;
    private final ClientServiceConfiguration clientServiceConfiguration;

    public AgentPowerToolCallback(ClientServiceConfiguration clientServiceConfiguration, String functionName) {
        this.clientServiceConfiguration = clientServiceConfiguration;
        this.toolDefinition = getToolDefinitionInterval(clientServiceConfiguration, functionName);
    }
    @Override
    public @Nonnull ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public String call(String toolInput) {
        return callClientFunction(toolInput);
    }

    private String callClientFunction(String toolInput) {
        return null;
    }

    private ToolDefinition getToolDefinitionInterval(ClientServiceConfiguration clientServiceConfiguration, String functionName) {
        // TODO 发送消息给客户端 触发客户端调用客户端服务接口
        Globals.Client.sendMessage(ServerSentEvent.builder()
                .event(Globals.Const.TOOL_CALL)
                .data(JSON.toJSONString(
                        Map.of(
                                "requestId", Globals.RequestContext.getRequestId(),
                                "configurationId", clientServiceConfiguration.getId(),
                                "functionName", functionName,
                                "serviceUrl", clientServiceConfiguration.getServiceUrl(),
                                "headers", clientServiceConfiguration.getHeaders(),
                                "auth", Globals.RSAUtil.encrypt("RSA",
                                        Globals.User.getLoginUser().getId(),
                                        clientServiceConfiguration.getServicePublicKey())
                        )
                )).build());
        // TODO 接收客户端服务回调的结果

        DefaultToolDefinition.builder()
                .name(function.functionName())
                .description(function.functionDesc())
                .inputSchema(function.functionParamSchema())
                .build()
        return null;
    }

    public static int receiveRequestResult(FunctionRequest request) {
        // TODO handle result, normally is string content
        return StatusCode.REQUEST_ABORT;
    }
}
