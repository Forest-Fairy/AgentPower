package org.agentpower.agent.tool;

import org.agentpower.configuration.agent.AgentModelConfiguration;
import org.agentpower.configuration.platform.provider.PlatformProvider;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

public class AgentPowerChatModelDelegate implements ChatModel {
    private final String requestId;
    private final AgentModelConfiguration agentModelConfiguration;
    private final ChatModel chatModel;
    public AgentPowerChatModelDelegate(String requestId, AgentModelConfiguration agentModelConfiguration) {
        this.requestId = requestId;
        this.agentModelConfiguration = agentModelConfiguration;
        this.chatModel = PlatformProvider.GetModel(agentModelConfiguration);
    }

    public String getRequestId() {
        return requestId;
    }

    public AgentModelConfiguration getAgentModelConfiguration() {
        return agentModelConfiguration;
    }

    public ChatModel getChatModel() {
        return chatModel;
    }

    @Override
    public String call(String message) {
        return chatModel.call(message);
    }

    @Override
    public String call(Message... messages) {
        return chatModel.call(messages);
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return chatModel.call(prompt);
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return chatModel.getDefaultOptions();
    }

    @Override
    public Flux<String> stream(String message) {
        return chatModel.stream(message);
    }

    @Override
    public Flux<String> stream(Message... messages) {
        return chatModel.stream(messages);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return chatModel.stream(prompt);
    }
}
