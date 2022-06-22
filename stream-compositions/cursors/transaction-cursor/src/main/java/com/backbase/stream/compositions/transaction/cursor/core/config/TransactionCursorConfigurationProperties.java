package com.backbase.stream.compositions.transaction.cursor.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("backbase.stream.transaction-cursor")
public class TransactionCursorConfigurationProperties {

    private TransactionIdPersistence transactionIdPersistence;

    @Data
    public static class TransactionIdPersistence {
        private boolean enabled;
    }
}
