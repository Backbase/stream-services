package com.backbase.stream.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backbase.stream.customer-access-groups")
public record CustomerAccessGroupConfigurationProperties(
    boolean enabled
) { }
