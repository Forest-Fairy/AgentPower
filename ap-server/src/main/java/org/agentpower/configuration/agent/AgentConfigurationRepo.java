package org.agentpower.configuration.agent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentConfigurationRepo extends JpaRepository<AgentConfiguration, String> {
}
