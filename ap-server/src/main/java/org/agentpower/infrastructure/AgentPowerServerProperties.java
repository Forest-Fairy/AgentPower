package org.agentpower.infrastructure;

import lombok.AllArgsConstructor;
import org.agentpower.api.Constants;
import org.springframework.boot.context.properties.ConfigurationProperties;

@AllArgsConstructor
@ConfigurationProperties(Constants.CONFIG_PREFIX)
public class AgentPowerServerProperties {

    private final Integer port;
    private final String serverId;


}