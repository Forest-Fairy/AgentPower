package org.agentpower.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(AgentPowerProperties.CONFIG_PREFIX)
public class AgentPowerProperties {
	public static final String CONFIG_PREFIX = "agent-power";



}