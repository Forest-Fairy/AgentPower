package org.agentpower.configuration.resource.provider;

import org.agentpower.configuration.resource.ResourceProviderConfiguration;
import org.springframework.ai.model.Media;
import org.springframework.core.io.Resource;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class ResourceProvider {
    private static final Map<String, ResourceProvider> providers = new HashMap<>(16);
    protected ResourceProvider() {
        ResourceProvider old = providers.put(this.type(), this);
        if (old != null) {
            throw new IllegalArgumentException("Duplicate provider type: " + this.type()
                    + " among " + old.getClass().getName() + " and " + this.getClass().getName());
        }
    }
    public abstract String type();

    public abstract String desc();

    public abstract Media getSource(ResourceProviderConfiguration resourceProviderConfiguration, String resourceId);

    public static Collection<ResourceProvider> providers() {
        return providers.values();
    }

    public static ResourceProvider getProvider(String type) {
        return providers.get(type);
    }
}
