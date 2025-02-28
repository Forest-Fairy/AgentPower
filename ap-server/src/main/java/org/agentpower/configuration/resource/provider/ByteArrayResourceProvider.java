package org.agentpower.configuration.resource.provider;

import org.agentpower.configuration.resource.ResourceProviderConfiguration;
import org.springframework.core.io.ByteArrayResource;

public abstract class ByteArrayResourceProvider extends ResourceProvider<ByteArrayResource> {
    @Override
    public final ByteArrayResource getSource(ResourceProviderConfiguration resourceProviderConfiguration, String configId) {
        return new ByteArrayResource(getByteArraySource(resourceProviderConfiguration, configId));
    }

    protected abstract byte[] getByteArraySource(ResourceProviderConfiguration resourceProviderConfiguration, String configId);
}
