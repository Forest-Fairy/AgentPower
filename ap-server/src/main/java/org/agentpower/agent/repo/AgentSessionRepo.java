package org.agentpower.agent.repo;

import org.agentpower.agent.model.AgentSessionModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentSessionRepo extends JpaRepository<AgentSessionModel, String> {
}
