package org.agentpower.service.configuration;

import org.agentpower.api.Constants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(Constants.CONFIG_PREFIX)
public class AgentPowerBaseProperties {
    @NestedConfigurationProperty
    private final ClientProperties client = new ClientProperties();
    @NestedConfigurationProperty
    private final ServerProperties server = new ServerProperties();

    public static class ClientProperties {
        private Integer port;
    }


    public static class ServerProperties {
        private Integer port;
    }
}
