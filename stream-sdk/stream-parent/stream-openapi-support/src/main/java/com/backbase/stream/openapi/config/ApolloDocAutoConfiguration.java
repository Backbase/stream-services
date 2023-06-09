package com.backbase.stream.openapi.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Auto Configuration for Swagger UI.
 */
@Configuration
@EnableConfigurationProperties(SwaggerUiConfigProperties.class)
@ConditionalOnProperty(name = "backbase.swagger-ui.enabled", matchIfMissing = true)
@ComponentScan(basePackages = {"com.backbase.stream.openapi"})
public class ApolloDocAutoConfiguration {

}
