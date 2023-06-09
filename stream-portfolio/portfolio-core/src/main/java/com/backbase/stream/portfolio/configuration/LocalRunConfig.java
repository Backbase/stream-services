package com.backbase.stream.portfolio.configuration;

import com.backbase.buildingblocks.webclient.InterServiceWebClientConfiguration;
import com.backbase.buildingblocks.webclient.InterServiceWebClientCustomizer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.text.DateFormat;
import java.util.TimeZone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;

/**
 * This configurations can avoid having oAuth server. Config can be used with a config:
 *
 * <p>spring.autoconfigure.exclude=com.backbase.buildingblocks.webclient.InterServiceWebClientConfiguration
 */
@Configuration
@ConditionalOnMissingBean(InterServiceWebClientConfiguration.class)
@Slf4j
public class LocalRunConfig {

  @Bean
  @ConditionalOnMissingBean
  public ObjectMapper objectMapper(DateFormat dateFormat) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setDateFormat(dateFormat);
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

  @Bean
  @ConditionalOnMissingBean
  public DateFormat dateFormat() {
    DateFormat dateFormat = new StdDateFormat();
    dateFormat.setTimeZone(TimeZone.getDefault());
    return dateFormat;
  }

  @Bean({"interServiceWebClient"})
  @ConditionalOnMissingBean(name = {"interServiceWebClient"})
  public WebClient interServiceWebClient(
      ObjectProvider<InterServiceWebClientCustomizer> interServiceWebClientCustomizers,
      Builder builder) {
    builder.defaultHeader("Content-Type", new String[] {MediaType.APPLICATION_JSON.toString()});
    builder.defaultHeader("Accept", new String[] {MediaType.APPLICATION_JSON.toString()});
    interServiceWebClientCustomizers
        .orderedStream()
        .forEach(
            (customizer) -> {
              customizer.customize(builder);
            });
    return builder.build();
  }
}
