package org.agentpower.configuration;

import com.alibaba.fastjson2.JSON;
import lombok.AllArgsConstructor;
import org.agentpower.configuration.agent.AgentModelConfiguration;
import org.agentpower.configuration.agent.AgentModelConfigurationRepo;
import org.agentpower.configuration.client.ClientServiceConfiguration;
import org.agentpower.configuration.client.ClientServiceConfigurationRepo;
import org.agentpower.configuration.resource.ResourceProviderConfiguration;
import org.agentpower.configuration.resource.ResourceProviderConfigurationRepo;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ConfigurationService {
    private final ClientServiceConfigurationRepo clientServiceConfigurationRepo;
    private final AgentModelConfigurationRepo agentModelConfigurationRepo;
    private final ResourceProviderConfigurationRepo resourceProviderConfigurationRepo;

    public ClientServiceConfiguration getClientServiceConfiguration(String clientServiceConfigId) {
        return clientServiceConfigurationRepo.findById(clientServiceConfigId)
                .orElse(JSON.parseObject("{'id': '" + clientServiceConfigId + "', 'serviceUrl': 'unknown'}",
                        ClientServiceConfiguration.class));
    }

    public AgentModelConfiguration getAgentModelConfiguration(String agentModelConfigId) {
        return agentModelConfigurationRepo.findById(agentModelConfigId)
                .orElse(JSON.parseObject("{'id': '" + agentModelConfigId + "', 'agentPlatform': 'unknown'}",
                        AgentModelConfiguration.class));
    }

    public ResourceProviderConfiguration getResourceProviderConfiguration(String resourceProviderConfigId) {
        return resourceProviderConfigurationRepo.findById(resourceProviderConfigId)
                .orElse(JSON.parseObject("{'id': '" + resourceProviderConfigId + "', 'type': 'unknown'}",
                        ResourceProviderConfiguration.class));
    }
}
