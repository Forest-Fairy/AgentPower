package org.agentpower.agent.tool;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSON;
import lombok.AllArgsConstructor;
import org.agentpower.agent.AgentChatHelper;
import org.agentpower.agent.model.ChatMessageModel;
import org.agentpower.agent.service.AgentChatService;
import org.agentpower.api.message.ChatMessageObject;
import org.agentpower.configuration.ConfigurationService;
import org.agentpower.configuration.client.ClientServiceConfiguration;
import org.agentpower.infrastracture.Globals;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Optional;

@AllArgsConstructor
public class AgentPowerToolCallbackResolver implements ToolCallbackResolver {
    private final GenericApplicationContext applicationContext;
    private final ConfigurationService configurationService;
    private final AgentChatService chatService;

    @Override
    public FunctionCallback resolve(String toolName) {
        AgentChatHelper.Function.FunctionNameInfo functionNameInfo = AgentChatHelper.Function.unwrapFunctionName(toolName);
        if (functionNameInfo == null) {
            return null;
        }
        return Optional.ofNullable(functionNameInfo.clientServiceId)
                .map(configurationService::getClientServiceConfiguration)
                .map(cs -> new AgentPowerToolCallback(this, cs, functionNameInfo.functionName))
                .orElse(null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String callPrompt(String requestId, String loginUserId,
                             ClientServiceConfiguration clientServiceConfiguration,
                             ChatMessageObject newMessageObject) {
        AgentPowerChatModelDelegate chatModelDelegate = AgentChatHelper.Runtime.getChatModelDelegate(requestId);
        String chatModelId = newMessageObject.setting().clientAgentModelId();
        AgentPowerChatModelDelegate chatModel;
        if (chatModelId != null && !chatModelId.equals(chatModelDelegate.getAgentModelConfiguration().getId())) {
            // 通过新模型调用
            chatModel = new AgentPowerChatModelDelegate(requestId, configurationService.getAgentModelConfiguration(chatModelId));
        } else {
            chatModel = chatModelDelegate;
        }
        return Optional.of(ChatMessageModel.builder())
                .map(builder -> builder
                        .requestId(Globals.RequestContext.getRequestId())
                        .messageType(MessageType.SYSTEM.getValue())
                        .sessionId(newMessageObject.sessionId())
                        .textContent(newMessageObject.textContent())
                        .agentModelConfigurationId(newMessageObject.setting().clientAgentModelId())
                        .clientAgentServiceConfigurationId(newMessageObject.setting().clientAgentServiceId())
                        .resourceProviders(JSON.toJSONString(newMessageObject.setting().resourceProviders()))
                        .userId(loginUserId)
                        .createdBy(loginUserId)
                        .createdTime(DateUtil.now())
                        .build())
                .map(messageModel -> chatService.prompt(chatModel, messageModel,))
                                .orElse("");
    }

    public static class Builder {
        private GenericApplicationContext applicationContext;
        public AgentPowerToolCallbackResolver build() {
            return new AgentPowerToolCallbackResolver(applicationContext);
        }

        public Builder applicationContext(GenericApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
            return this;
        }
    }
}
