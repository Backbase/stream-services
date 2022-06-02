package com.backbase.stream.compositions.transaction.core.config;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.compositions.transaction")
public class TransactionConfigurationProperties {
    private String integrationBaseUrl = "http://transaction-ingestion-integration:8080";
    private Events events;
    private Cursor cursor;


    @Data
    @NoArgsConstructor
    public static class Events {
        private Boolean enableCompleted = Boolean.FALSE;
        private Boolean enableFailed = Boolean.FALSE;
    }

    @Data
    @NoArgsConstructor
    public static class Cursor {
        private Boolean enabled = Boolean.FALSE;
        private String baseUrl = "http://product-cursor:9000";
    }
}
