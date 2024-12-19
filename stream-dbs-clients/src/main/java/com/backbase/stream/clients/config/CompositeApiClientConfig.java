package com.backbase.stream.clients.config;

import com.backbase.buildingblocks.webclient.client.ApiClientConfig;
import jakarta.validation.constraints.Positive;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer.Factory;
import org.springframework.validation.annotation.Validated;

/**
 * This specialization make it flexible to use direct uris when client-side load balancing is not
 * desired/supported.
 *
 * <p>In order to use the `direct-uri` property, the value of `spring.cloud.loadbalancer.enabled`
 * needs to be set to false in services with support to client-side load balancing.
 */
@Validated
public class CompositeApiClientConfig extends ApiClientConfig {

  /** Direct uri used as base path when load balancing is not available. */
  private String directUri;

  /** Fallback when ports are the same for all service clients. */
  @Positive
  @Value("${backbase.communication.http.default-service-port:}")
  private Integer defaultServicePort;

  /** Indicator whether load balancing is enabled or not. */
  private boolean loadBalancerEnabled;

  public CompositeApiClientConfig(String serviceId) {
    super(serviceId);
  }

  @Override
  public Integer getServicePort() {
    if (super.getServicePort() == null && getDefaultServicePort() != null) {
      return getDefaultServicePort();
    }
    return super.getServicePort();
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

  public Integer getDefaultServicePort() {
    return defaultServicePort;
  }

  public void setDefaultServicePort(Integer defaultServicePort) {
    this.defaultServicePort = defaultServicePort;
  }
}
