package com.backbase.stream;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.backbase.stream.config.BootstrapConfigurationProperties;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.Assert;

@SpringBootTest
@ActiveProfiles({"it", "moustache-bank", "moustache-bank-subsidiaries"})
public class SetupLegalEntityHierarchyTaskApplicationIT {

    @RegisterExtension
    static WireMockExtension wiremock =
        WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    @Autowired
    BootstrapConfigurationProperties configuration;

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        String wiremockUrl = String.format("http://localhost:%d", wiremock.getPort());
        registry.add("spring.zipkin.base-url", () -> wiremockUrl);
        registry.add(
            "spring.cloud.discovery.client.simple.instances.token-converter[0].uri", () -> wiremockUrl);
        registry.add(
            "spring.cloud.discovery.client.simple.instances.user-manager[0].uri", () -> wiremockUrl);
        registry.add(
            "spring.cloud.discovery.client.simple.instances.user-manager[0].metadata.contextPath",
            () -> "/user-manager");
        registry.add(
            "spring.cloud.discovery.client.simple.instances.access-control[0].uri", () -> wiremockUrl);
        registry.add(
            "spring.cloud.discovery.client.simple.instances.access-control[0].metadata.contextPath",
            () -> "/access-control");
        registry.add(
            "spring.cloud.discovery.client.simple.instances.arrangement-manager[0].uri",
            () -> wiremockUrl);
        registry.add(
            "spring.cloud.discovery.client.simple.instances.arrangement-manager[0].metadata.contextPath",
            () -> "/arrangement-manager");
    }

    @Test
    void contextLoads() {
        // Triggers the CommandLineRunner which will run the boostrap task to be validated by the
        // WireMock assertions.
        Assert.notEmpty(
            configuration.getLegalEntity().getSubsidiaries(),
            "At least one subsidiary should be present.");
        Assert.notNull(
            configuration.getLegalEntity().getName(), "Legal entity name should be present.");
    }
}
