package org.agentpower.configuration.agent.provider;

import org.agentpower.configuration.agent.AgentModelConfiguration;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AgentModelProvider {
    private static final Map<String, AgentModelProvider> PROVIDERS = new ConcurrentHashMap<>();
    protected AgentModelProvider() {
        AgentModelProvider old = PROVIDERS.put(providerName(), this);
        if (old != null) {
            throw new IllegalArgumentException("Duplicate provider type: " + this.providerName()
                    + " among " + old.getClass().getName() + " and " + this.getClass().getName());
        }
    }
    public abstract String providerName();

    public abstract ChatModel getModel(AgentModelConfiguration modelConfiguration);

    public static ChatModel GetModel(AgentModelConfiguration modelConfiguration) {
        return Optional.of(PROVIDERS.get(modelConfiguration.getAgentPlatform()))
                .map(register -> register.getModel(modelConfiguration))
                .orElse(null);
    }
    public static List<String> getPlatforms() {
        return List.copyOf(PROVIDERS.keySet());
    }
}
