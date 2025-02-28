package org.agentpower.configuration.platform.provider;

import org.agentpower.configuration.agent.AgentModelConfiguration;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class PlatformProvider {
    private static final Map<String, PlatformProvider> PLATFORM_PROVIDERS = new ConcurrentHashMap<>();
    protected PlatformProvider() {
        PLATFORM_PROVIDERS.put(providerName(), this);
    }
    public abstract String providerName();

    public abstract ChatModel getModel(AgentModelConfiguration modelConfiguration);

    public static ChatModel GetModel(AgentModelConfiguration modelConfiguration) {
        return Optional.of(PLATFORM_PROVIDERS.get(modelConfiguration.getAgentPlatform()))
                .map(register -> register.getModel(modelConfiguration))
                .orElse(null);
    }
    public static List<String> getPlatforms() {
        return List.copyOf(PLATFORM_PROVIDERS.keySet());
    }
}
