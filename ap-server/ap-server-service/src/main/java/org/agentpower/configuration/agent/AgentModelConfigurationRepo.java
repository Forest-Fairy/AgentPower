package org.agentpower.configuration.agent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentModelConfigurationRepo extends JpaRepository<AgentModelConfiguration, String> {
}
