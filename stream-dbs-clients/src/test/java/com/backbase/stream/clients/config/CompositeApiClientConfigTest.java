package com.backbase.stream.clients.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.backbase.buildingblocks.webclient.InterServiceWebClientConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer.Factory;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
class CompositeApiClientConfigTest {

    @Mock
    private Factory<ServiceInstance> loadBalancerFactory;

    private ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void shouldReturnServiceIdWhenWithLoadBalancerTest() {
        contextRunner
            .withBean(Factory.class, () -> loadBalancerFactory)
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withBean(CompositeApiClientConfig.class, "banking-service")
            .run(context -> {
                var config = context.getBean(CompositeApiClientConfig.class);
                assertEquals("http://banking-service", config.createBasePath());
            });
    }

    @Test
    void shouldReturnServiceIdWhenWithoutLoadBalancerAndWithoutDirectUriTest() {
        contextRunner
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withUserConfiguration(UserProfileManagerClientConfig.class)
            .run(context -> {
                var config = context.getBean(UserProfileManagerClientConfig.class);
                assertEquals("http://user-profile-manager", config.createBasePath());
            });
    }

    @Test
    void shouldReturnDirectUriWhenWithoutLoadBalancerAndWithDirectUriTest() {
        contextRunner
            .withPropertyValues("backbase.communication.services.user.profile.direct-uri=http://my-custom-uri/context")
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withBean(UserProfileManagerClientConfig.class)
            .run(context -> {
                var config = context.getBean(UserProfileManagerClientConfig.class);
                assertEquals("http://my-custom-uri/context", config.createBasePath());
            });
    }

    @Test
    void shouldReturnServiceIdWhenWithLoadBalancerAndWithDirectUriTest() {
        contextRunner
            .withPropertyValues("backbase.communication.services.user.profile.direct-uri=http://my-custom-uri/context")
            .withBean(Factory.class, () -> loadBalancerFactory)
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withUserConfiguration(UserProfileManagerClientConfig.class)
            .run(context -> {
                var config = context.getBean(UserProfileManagerClientConfig.class);
                assertEquals("http://user-profile-manager", config.createBasePath());
            });
    }

    @Test
    void shouldNotReturnDefaultServicePortWhenServicePortIsSetTest() {
        contextRunner
            .withPropertyValues("backbase.communication.http.default-service-port=8181",
                "backbase.communication.services.user.profile.service-port=8080")
            .withBean(Factory.class, () -> loadBalancerFactory)
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withUserConfiguration(UserProfileManagerClientConfig.class)
            .run(context -> {
                var config = context.getBean(UserProfileManagerClientConfig.class);
                assertEquals("http://user-profile-manager:8080", config.createBasePath());
            });
    }

    @Test
    void shouldReturnDefaultServicePortWhenServicePortIsEmptyTest() {
        contextRunner
            .withPropertyValues("backbase.communication.http.default-service-port=8181")
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withUserConfiguration(UserProfileManagerClientConfig.class)
            .run(context -> {
                var config = context.getBean(UserProfileManagerClientConfig.class);
                assertEquals("http://user-profile-manager:8181", config.createBasePath());
            });
    }
    @Test
    void shouldReturnServiceIdWhenCustomerProfileWithLoadBalancerTest() {
        contextRunner
            .withBean(Factory.class, () -> loadBalancerFactory)
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withUserConfiguration(CustomerProfileClientConfig.class)
            .run(context -> {
                var config = context.getBean(CustomerProfileClientConfig.class);
                assertEquals("http://customer-profile", config.createBasePath());
            });
    }

    @Test
    void shouldReturnDirectUriWhenCustomerProfileWithoutLoadBalancerAndWithDirectUriTest() {
        contextRunner
            .withPropertyValues("backbase.communication.services.customer-profile.direct-uri=http://custom-profile-uri/api")
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withBean(CustomerProfileClientConfig.class)
            .run(context -> {
                var config = context.getBean(CustomerProfileClientConfig.class);
                assertEquals("http://custom-profile-uri/api", config.createBasePath());
            });
    }

    @Test
    void shouldReturnServiceIdWhenCustomerProfileWithLoadBalancerAndWithDirectUriTest() {
        contextRunner
            .withPropertyValues("backbase.communication.services.customer-profile.direct-uri=http://custom-profile-uri/api")
            .withBean(Factory.class, () -> loadBalancerFactory)
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withUserConfiguration(CustomerProfileClientConfig.class)
            .run(context -> {
                var config = context.getBean(CustomerProfileClientConfig.class);
                assertEquals("http://customer-profile", config.createBasePath());
            });
    }

    @Test
    void shouldNotReturnDefaultServicePortWhenCustomerProfileServicePortIsSetTest() {
        contextRunner
            .withPropertyValues("backbase.communication.http.default-service-port=8181",
                "backbase.communication.services.customer-profile.service-port=8080")
            .withBean(Factory.class, () -> loadBalancerFactory)
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withUserConfiguration(CustomerProfileClientConfig.class)
            .run(context -> {
                var config = context.getBean(CustomerProfileClientConfig.class);
                assertEquals("http://customer-profile:8080", config.createBasePath());
            });
    }

    @Test
    void shouldReturnDefaultServicePortWhenCustomerProfileServicePortIsEmptyTest() {
        contextRunner
            .withPropertyValues("backbase.communication.http.default-service-port=8080")
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withUserConfiguration(CustomerProfileClientConfig.class)
            .run(context -> {
                var config = context.getBean(CustomerProfileClientConfig.class);
                assertEquals("http://customer-profile:8080", config.createBasePath());
            });
    }

}
