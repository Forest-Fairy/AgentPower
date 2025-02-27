package org.agentpower.agent.tool;

import lombok.AllArgsConstructor;
import org.agentpower.api.AgentPowerFunction;
import org.agentpower.configuration.ConfigurationService;
import org.agentpower.configuration.client.ClientServiceConfigurationService;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@AllArgsConstructor
public class AgentPowerToolCallbackResolver implements ToolCallbackResolver {

    public static final String callbackPrefix = "AgentPower-";
    private final GenericApplicationContext applicationContext;
    private final ConfigurationService configurationService;
    private final ClientServiceConfigurationService clientServiceConfigurationService;
    private final RestTemplate restTemplate;

    @Override
    public FunctionCallback resolve(String toolName) {
        if (!toolName.startsWith(callbackPrefix)) {
            return null;
        }
        // AgentPower-clientServiceId-functionName
        String[] toolInfos = toolName.split("-", 3);
        String clientServiceId = toolInfos[1];
        String functionName = toolInfos[2];
        // TODO callRemote client service

        return Optional
                .ofNullable(configurationService.getClientServiceConfiguration(clientServiceId))
                .map(AgentPowerToolCallback::new).orElse(null);
    }

    public static Builder builder() {
        return new Builder();
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
