package example;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * Helper for avoiding ambiguous reference to overloaded definition
 */
public class ResourceConfigWrapper {

    private ResourceConfig config;

    public ResourceConfigWrapper(ResourceConfig resourceConfig) {
        this.config = resourceConfig;
    }

    public void register(Object comp) {
        config.register(comp);
    }
}
