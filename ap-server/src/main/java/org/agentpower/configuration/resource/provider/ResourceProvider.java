package org.agentpower.configuration.resource.provider;

import org.agentpower.configuration.resource.ResourceProviderConfiguration;
import org.springframework.core.io.Resource;

import java.util.HashMap;
import java.util.Map;

public abstract class ResourceProvider<T extends Resource> {
    private static final Map<String, ResourceProvider<?>> providers = new HashMap<>(16);
    protected ResourceProvider() {
        ResourceProvider<?> old = providers.put(this.type(), this);
        if (old != null) {
            throw new IllegalArgumentException("Duplicate provider type: " + this.type()
                    + " among " + old.getClass().getName() + " and " + this.getClass().getName());
        }
    }
    public abstract String type();
    public abstract T getSource(ResourceProviderConfiguration resourceProviderConfiguration, String configId);

    public static <T extends Resource> ResourceProvider<T> getProvider(String type) {
        return (ResourceProvider<T>) providers.get(type);
    }
}
