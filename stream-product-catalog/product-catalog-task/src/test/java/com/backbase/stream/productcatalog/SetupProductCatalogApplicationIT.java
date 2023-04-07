package com.backbase.stream.productcatalog;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.backbase.stream.productcatalog.configuration.ProductCatalogConfigurationProperties;
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
@ActiveProfiles({"it", "moustache-bank"})
public class SetupProductCatalogApplicationIT {

  @RegisterExtension
  static WireMockExtension wiremock =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @Autowired ProductCatalogConfigurationProperties configuration;

  @DynamicPropertySource
  static void registerDynamicProperties(DynamicPropertyRegistry registry) {
    String wiremockUrl = String.format("http://localhost:%d", wiremock.getPort());
    registry.add("spring.zipkin.base-url", () -> wiremockUrl);
    registry.add(
        "spring.cloud.discovery.client.simple.instances.token-converter[0].uri", () -> wiremockUrl);
    registry.add(
        "spring.cloud.discovery.client.simple.instances.arrangement-manager[0].uri",
        () -> wiremockUrl);
  }

  @Test
  void contextLoads() {
    // Triggers the CommandLineRunner which will run the boostrap task to be validated by the
    // WireMock assertions.
    Assert.notNull(configuration.getProductCatalog(), "Product catalog should be present.");
    Assert.notEmpty(
        configuration.getProductCatalog().getProductTypes(),
        "At least one type should be present.");
  }
}
