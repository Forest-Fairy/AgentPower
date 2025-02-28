package org.agentpower.agent.repo;

import org.agentpower.agent.model.ChatMessageModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepo extends JpaRepository<ChatMessageModel, String> {
}
