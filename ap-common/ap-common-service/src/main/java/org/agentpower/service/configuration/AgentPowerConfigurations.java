package org.agentpower.service.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        AgentPowerBaseProperties.class
})
public class AgentPowerConfigurations {

}
