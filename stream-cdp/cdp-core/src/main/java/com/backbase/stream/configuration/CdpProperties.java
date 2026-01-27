package com.backbase.stream.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backbase.stream.audiences.segmentation.user-kind")
public record CdpProperties(
    boolean enabled,
    String defaultCustomerCategory
) {

}
