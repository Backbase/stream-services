package com.backbase.stream;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Default set of properties loaded via the spring-cloud-stream-config.properties file.
 */
@Configuration
@PropertySource("classpath:spring-cloud-stream-config.properties")
public class DefaultSpringCloudStreamProperties {

}