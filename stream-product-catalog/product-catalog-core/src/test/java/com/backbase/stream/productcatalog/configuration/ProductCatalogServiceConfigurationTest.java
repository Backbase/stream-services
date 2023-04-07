package com.backbase.stream.productcatalog.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.backbase.buildingblocks.webclient.InterServiceWebClientConfiguration;
import com.backbase.stream.clients.autoconfigure.DbsApiClientsAutoConfiguration;
import com.backbase.stream.productcatalog.ProductCatalogService;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
class ProductCatalogServiceConfigurationTest {

    ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void configurationTest() {
        contextRunner
                .withBean(WebClientAutoConfiguration.class)
                .withBean(DbsApiClientsAutoConfiguration.class)
                .withBean(InterServiceWebClientConfiguration.class)
                .withUserConfiguration(ProductCatalogServiceConfiguration.class)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(ProductCatalogService.class);
                        });
    }
}
