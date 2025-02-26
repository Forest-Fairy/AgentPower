package org.agentpower.configuration.repository;

import org.agentpower.configuration.model.UserAgentConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAgentConfigurationRepo extends JpaRepository<UserAgentConfiguration, String> {
}
