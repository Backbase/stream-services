package com.backbase.stream.compositions.product.core.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.compositions.product")
public class ProductConfigurationProperties {
    private Boolean enableCompletedEvents = true;
    private Boolean enableFailedEvents = true;
    private String productIntegrationUrl = "http://product-ingestion-integration:8080";
}
