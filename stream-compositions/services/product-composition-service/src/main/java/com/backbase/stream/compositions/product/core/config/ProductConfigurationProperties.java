package com.backbase.stream.compositions.product.core.config;

import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.product.task.BatchProductIngestionMode;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.compositions.product")
public class ProductConfigurationProperties {
    private String integrationBaseUrl = "http://product-ingestion-integration:8080";
    private Chains chains;
    private Events events;
    private Cursor cursor;
    private IngestionMode ingestionMode = new IngestionMode();

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

    @Data
    @NoArgsConstructor
    public static class Chains {
        private TransactionComposition transactionComposition;
        private PaymentOrderComposition paymentOrderComposition;
    }

    @Data
    @NoArgsConstructor
    public static class IngestionMode {
        private BatchProductIngestionMode.FunctionGroupsMode functionGroups = BatchProductIngestionMode.FunctionGroupsMode.UPSERT;
        private BatchProductIngestionMode.DataGroupsMode dataGroups = BatchProductIngestionMode.DataGroupsMode.UPSERT;
        private BatchProductIngestionMode.ArrangementsMode arrangements = BatchProductIngestionMode.ArrangementsMode.UPSERT;
    }

    @Data
    public static abstract class BaseComposition {
        private Boolean enabled = Boolean.FALSE;
        private String baseUrl = "http://localhost:9003/";
        private Boolean async = Boolean.FALSE;
    }

    @NoArgsConstructor
    @Data
    public static class TransactionComposition extends BaseComposition {
        private List<String> excludeProductTypeExternalIds = new ArrayList<>();
    }

    @NoArgsConstructor
    @Data
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
}
