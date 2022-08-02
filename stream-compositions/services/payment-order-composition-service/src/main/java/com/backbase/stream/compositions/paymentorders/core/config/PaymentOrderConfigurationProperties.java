package com.backbase.stream.compositions.paymentorders.core.config;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.compositions.paymentorder")
public class PaymentOrderConfigurationProperties {
    private String integrationBaseUrl = "http://payment-orders-ingestion-integration:8080";
    private Events events;
//    private Cursor cursor;
    private Integer defaultStartOffsetInDays;

    @Data
    @NoArgsConstructor
    public static class Events {
        private Boolean enableCompleted = Boolean.FALSE;
        private Boolean enableFailed = Boolean.FALSE;
    }
//
//    @Data
//    @NoArgsConstructor
//    public static class Cursor {
//        private Boolean enabled = Boolean.FALSE;
//        private Boolean transactionIdsFilterEnabled = Boolean.FALSE;
//        private String baseUrl = "http://product-cursor:9000";
//    }
//
//    public final boolean isCursorEnabled() {
//        return Boolean.TRUE.equals(cursor.getEnabled());
//    }
//
//    public final boolean isTransactionIdsFilterEnabled() {
//        return Boolean.TRUE.equals(cursor.getTransactionIdsFilterEnabled());
//    }
}
