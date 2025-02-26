package org.agentpower.configuration.repository;

import org.agentpower.configuration.model.UserClientServiceConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserClientServiceConfigurationRepo extends JpaRepository<UserClientServiceConfiguration, String> {
}
