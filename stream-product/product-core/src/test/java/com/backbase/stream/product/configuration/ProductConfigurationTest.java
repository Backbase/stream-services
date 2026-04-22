package com.backbase.stream.product.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.backbase.buildingblocks.webclient.InterServiceWebClientConfiguration;
import com.backbase.stream.clients.autoconfigure.DbsApiClientsAutoConfiguration;
import com.backbase.stream.product.service.ArrangementService;
import com.backbase.stream.webclient.configuration.DbsWebClientConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
class ProductConfigurationTest {

    ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void configurationTest() {
        contextRunner
            .withBean(DbsWebClientConfiguration.class)
            .withBean(WebClientAutoConfiguration.class)
            .withBean(DbsApiClientsAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withUserConfiguration(ProductConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(ArrangementService.class);
            });
    }

}
