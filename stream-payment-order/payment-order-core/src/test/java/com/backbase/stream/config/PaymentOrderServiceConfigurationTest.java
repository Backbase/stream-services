package com.backbase.stream.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.backbase.buildingblocks.webclient.InterServiceWebClientConfiguration;
import com.backbase.stream.clients.autoconfigure.DbsApiClientsAutoConfiguration;
import com.backbase.stream.mappers.PaymentOrderTypeMapperImpl;
import com.backbase.stream.paymentorder.PaymentOrderTaskExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
class PaymentOrderServiceConfigurationTest {

  ApplicationContextRunner contextRunner = new ApplicationContextRunner();

  @Test
  void configurationTest() {
    contextRunner
        .withBean(PaymentOrderTypeMapperImpl.class)
        .withBean(WebClientAutoConfiguration.class)
        .withBean(DbsApiClientsAutoConfiguration.class)
        .withBean(InterServiceWebClientConfiguration.class)
        .withUserConfiguration(PaymentOrderServiceConfiguration.class)
        .withUserConfiguration(PaymentOrderTypeConfiguration.class)
        .withPropertyValues("backbase.stream.paymentorder.types=type1,type2,type3")
        .run(
            context -> {
              assertThat(context).hasSingleBean(PaymentOrderTaskExecutor.class);
            });
  }
}
