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
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
public class AgentPowerToolCallbackResolver implements ToolCallbackResolver {
    private final GenericApplicationContext applicationContext;
    private final ConfigurationService configurationService;
    private final AgentChatService chatService;

    @Override
    public FunctionCallback resolve(String toolName) {
        AgentChatHelper.FunctionInfo.FunctionNameInfo functionNameInfo = AgentChatHelper.FunctionInfo.unwrapFunctionName(toolName);
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
        AgentPowerChatModelDelegate chatModelToUse;
        if (chatModelId != null && !chatModelId.equals(chatModelDelegate.getAgentModelConfiguration().getId())) {
            // 通过新模型调用
            chatModelToUse = Optional.ofNullable(AgentChatHelper.Runtime.getChatModelDelegate(requestId, chatModelId))
                    .orElse(new AgentPowerChatModelDelegate(requestId, configurationService.getAgentModelConfiguration(chatModelId)));
        } else {
            chatModelToUse = chatModelDelegate;
        }
        ClientServiceConfiguration clientServiceToUse;
        if (newMessageObject.setting().clientAgentServiceId() != null && ! clientServiceConfiguration.getId().equals(newMessageObject.setting().clientAgentServiceId())) {
            clientServiceToUse = configurationService.getClientServiceConfiguration(newMessageObject.setting().clientAgentServiceId());
        } else {
            clientServiceToUse = clientServiceConfiguration;
        }
        return Optional.of(ChatMessageModel.builder())
                .map(builder -> builder
                        .requestId(Globals.RequestContext.getRequestId())
                        .messageType(MessageType.SYSTEM.getValue())
                        .sessionId(newMessageObject.sessionId())
                        .textContent(newMessageObject.textContent())
                        .agentModelConfigurationId(chatModelToUse.getAgentModelConfiguration().getId())
                        .clientAgentServiceConfigurationId(clientServiceToUse.getId())
                        .resourceProviders(JSON.toJSONString(newMessageObject.setting().resourceProviders()))
                        .userId(loginUserId)
                        .createdBy(loginUserId)
                        .createdTime(DateUtil.now())
                        .build())
                .map(messageModel -> chatService.prompt(chatModelToUse, messageModel,
                        AgentChatHelper.Prompt.getFunctions(requestId, loginUserId, clientServiceToUse).keySet(),
                        messageModel.getChatMemoryCouplesCount()))
                .stream().flatMap(Flux::toStream)
                .map(res -> res.getResult().getOutput().getText())
                .collect(Collectors.joining());

    }

    public static class Builder {
        private GenericApplicationContext applicationContext;
        private ConfigurationService configurationService;
        private AgentChatService chatService;
        public AgentPowerToolCallbackResolver build() {
            return new AgentPowerToolCallbackResolver(applicationContext, configurationService, chatService);
        }

        public Builder applicationContext(GenericApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
            return this;
        }
        public Builder configurationService(ConfigurationService configurationService) {
            this.configurationService = configurationService;
            return this;
        }
        public Builder chatService(AgentChatService chatService) {
            this.chatService = chatService;
            return this;
        }
    }
}
