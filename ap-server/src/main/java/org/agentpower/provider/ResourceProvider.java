package org.agentpower.provider;

import org.agentpower.configuration.provider.ResourceProviderConfiguration;

import java.util.HashMap;
import java.util.Map;

public abstract class ResourceProvider<T> {
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

    public static <T> ResourceProvider<T> getProvider(String type) {
        return (ResourceProvider<T>) providers.get(type);
    }
}
