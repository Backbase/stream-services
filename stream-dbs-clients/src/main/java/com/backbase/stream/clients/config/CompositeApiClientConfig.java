package com.backbase.stream.clients.config;

import com.backbase.buildingblocks.webclient.client.ApiClientConfig;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer.Factory;

/**
 * This specialization make it flexible to use direct uris when load balancing is not available.
 */
class CompositeApiClientConfig extends ApiClientConfig {

    /**
     * Direct uri used as base path when load balancing is not available.
     */
    private String directUri;

    /**
     * Indicator whether load balancing is enabled or not.
     */
    private boolean loadBalancerEnabled;

    public CompositeApiClientConfig(String serviceId) {
        super(serviceId);
    }

    @Override
    public String createBasePath() {
        if (!loadBalancerEnabled && getDirectUri() != null) {
            return getDirectUri();
        }
        return super.createBasePath();
    }

    @Autowired
    protected void setLoadBalancerEnabled(Optional<Factory<ServiceInstance>> loadBalancerFactory) {
        this.loadBalancerEnabled = loadBalancerFactory.isPresent();
    }

    public String getDirectUri() {
        return directUri;
    }

    public void setDirectUri(String directUri) {
        this.directUri = directUri;
    }

}
