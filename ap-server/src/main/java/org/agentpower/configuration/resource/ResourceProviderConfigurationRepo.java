package org.agentpower.configuration.resource;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceProviderConfigurationRepo extends JpaRepository<ResourceProviderConfiguration, String> {
}
