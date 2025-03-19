package org.agentpower.configuration.client;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientServiceConfigurationRepo extends JpaRepository<ClientServiceConfiguration, String> {
}
