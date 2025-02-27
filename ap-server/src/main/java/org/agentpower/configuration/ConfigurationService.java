package org.agentpower.configuration;

import com.alibaba.fastjson2.JSON;
import lombok.AllArgsConstructor;
import org.agentpower.configuration.client.ClientServiceConfiguration;
import org.agentpower.configuration.client.ClientServiceConfigurationRepo;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ConfigurationService {
    private ClientServiceConfigurationRepo clientServiceConfigurationRepo;
    public ClientServiceConfiguration getClientServiceConfiguration(String configId) {
        return clientServiceConfigurationRepo.findById(configId)
                .orElse(JSON.parseObject("{'serviceUrl': 'unknown'}",
                        ClientServiceConfiguration.class));
    }

}
