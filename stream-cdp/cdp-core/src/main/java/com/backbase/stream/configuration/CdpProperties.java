package com.backbase.stream.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backbase.stream.cdp")
public record CdpProperties(
    boolean enabled,
    String defaultCustomerCategory
) {

}
