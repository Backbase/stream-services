package com.backbase.stream.compositions.product.core.config;

import com.backbase.stream.compositions.product.core.model.RequestConfig;
import com.backbase.stream.product.task.BatchProductGroupTask;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Setter
@Getter
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.compositions.product")
public class ProductConfigurationProperties {
    private String integrationBaseUrl = "http://product-ingestion-integration:8080";
    private Chains chains;
    private Events events;
    private Cursor cursor;
    private BatchProductGroupTask.IngestionMode ingestionMode = BatchProductGroupTask.IngestionMode.UPDATE;

    @Data
    @SuperBuilder
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

    @Data
    @SuperBuilder
    @NoArgsConstructor
    public static class Chains {
        private TransactionComposition transactionComposition;
        private PaymentOrderComposition paymentOrderComposition;
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    public static abstract class BaseComposition {
        private Boolean enabled = Boolean.FALSE;
        private String baseUrl = "http://localhost:9003/";
        private Boolean async = Boolean.FALSE;
    }


    @Data
    @SuperBuilder
    @NoArgsConstructor
    public static class TransactionComposition extends BaseComposition {
        private List<String> excludeProductTypeExternalIds = new ArrayList<>();
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    public static class PaymentOrderComposition extends BaseComposition {
        private List<String> excludeProductTypeExternalIds = new ArrayList<>();
    }


    public boolean isCompletedEventEnabled() {
        return Boolean.TRUE.equals(events.getEnableCompleted());
    }

    public boolean isFailedEventEnabled() {
        return Boolean.TRUE.equals(events.getEnableFailed());
    }

    public boolean isTransactionChainEnabled() {
        return Boolean.TRUE.equals(chains.getTransactionComposition().getEnabled());
    }

    public boolean isTransactionChainAsync() {
        return Boolean.TRUE.equals(chains.getTransactionComposition().getAsync());
    }

    public boolean isPaymentOrderChainEnabled() {
        return Boolean.TRUE.equals(chains.getPaymentOrderComposition().getEnabled());
    }

    public boolean isPaymentOrderChainAsync() {
        return Boolean.TRUE.equals(chains.getPaymentOrderComposition().getAsync());
    }

    public boolean isTransactionChainEnabled(RequestConfig requestConfig) {
        return requestConfig == null || requestConfig.isTransactionChainEnabled().isEmpty()
                ? Boolean.TRUE.equals(chains.getTransactionComposition().getEnabled())
                : requestConfig.isTransactionChainEnabled().orElse(false);
    }


    public boolean isTransactionChainAsync(RequestConfig requestConfig) {
        return requestConfig == null || requestConfig.isTransactionChainAsync().isEmpty()
                ? Boolean.TRUE.equals(chains.getTransactionComposition().getAsync())
                : requestConfig.isTransactionChainAsync().orElse(false);
    }
}
