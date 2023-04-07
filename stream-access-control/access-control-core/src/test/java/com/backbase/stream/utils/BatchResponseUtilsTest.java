package com.backbase.stream.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.backbase.buildingblocks.webclient.InterServiceWebClientConfiguration;
import com.backbase.stream.clients.autoconfigure.DbsApiClientsAutoConfiguration;
import com.backbase.stream.configuration.AccessControlConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@SpringJUnitConfig
public class BatchResponseUtilsTest {

  ApplicationContextRunner contextRunner = new ApplicationContextRunner();

  @Test
  void shouldThrowExceptionWhenBatchIsInconsistentByDefault() {
    contextRunner
        .withBean(WebClientAutoConfiguration.class)
        .withBean(DbsApiClientsAutoConfiguration.class)
        .withBean(InterServiceWebClientConfiguration.class)
        .withUserConfiguration(AccessControlConfiguration.class)
        .run(
            context -> {
              assertThat(context)
                  .getBean(BatchResponseUtils.class)
                  .hasFieldOrProperty("validateAtomicResponse")
                  .extracting("validateAtomicResponse")
                  .isEqualTo(true);

              BatchResponseUtils utils = context.getBean(BatchResponseUtils.class);
              Assertions.assertThrows(
                  WebClientResponseException.class,
                  () -> utils.checkBatchResponseItem(null, null, "400", null, null),
                  "WebClientResponseException was expected");
            });
  }

  @Test
  void shouldNotThrowExceptionWhenBatchIsInconsistentIfDisabled() {
    contextRunner
        .withBean(WebClientAutoConfiguration.class)
        .withBean(DbsApiClientsAutoConfiguration.class)
        .withBean(InterServiceWebClientConfiguration.class)
        .withUserConfiguration(AccessControlConfiguration.class)
        .withPropertyValues("backbase.stream.dbs.batch.validate-atomic-response=false")
        .run(
            context -> {
              assertThat(context)
                  .getBean(BatchResponseUtils.class)
                  .hasFieldOrProperty("validateAtomicResponse")
                  .extracting("validateAtomicResponse")
                  .isEqualTo(false);

              BatchResponseUtils utils = context.getBean(BatchResponseUtils.class);
              String response =
                  utils.checkBatchResponseItem("Expected Response", null, "400", null, null);
              Assertions.assertEquals(
                  "Expected Response", response, "Validation should be disabled");
            });
  }
}
