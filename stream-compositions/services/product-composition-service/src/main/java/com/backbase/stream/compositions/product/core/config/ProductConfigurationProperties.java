package com.backbase.stream.compositions.product.core.config;

import com.backbase.stream.compositions.product.core.model.RequestConfig;
import com.backbase.stream.product.task.BatchProductIngestionMode;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.compositions.product")
public class ProductConfigurationProperties implements InitializingBean {

    private Chains chains = new Chains();
    private Events events = new Events();
    private Cursor cursor = new Cursor();
    private IngestionMode ingestionMode = new IngestionMode();

    public boolean isCompletedEventEnabled() {
        return Boolean.TRUE.equals(events.getEnableCompleted());
    }

    public boolean isFailedEventEnabled() {
        return Boolean.TRUE.equals(events.getEnableFailed());
    }

    public boolean isTransactionChainEnabled() {
        return Boolean.TRUE.equals(chains.getTransactionComposition().getEnabled())
            || Boolean.TRUE.equals(chains.getTransactionManager().getEnabled());
    }

    public boolean isTransactionChainEnabled(RequestConfig requestConfig) {
        return requestConfig == null || requestConfig.isTransactionChainEnabled().isEmpty()
            ? Boolean.TRUE.equals(chains.getTransactionComposition().getEnabled())
            || Boolean.TRUE.equals(chains.getTransactionManager().getEnabled())
            : requestConfig.isTransactionChainEnabled().orElse(false);
    }

    public boolean isTransactionChainAsync(RequestConfig requestConfig) {
        return requestConfig == null || requestConfig.isTransactionChainAsync().isEmpty()
            ? Boolean.TRUE.equals(chains.getTransactionComposition().getAsync())
            || Boolean.TRUE.equals(chains.getTransactionManager().getAsync())
            : requestConfig.isTransactionChainAsync().orElse(false);
    }

    public boolean isTransactionChainAsync() {
        return Boolean.TRUE.equals(chains.getTransactionComposition().getAsync())
            || Boolean.TRUE.equals(chains.getTransactionManager().getAsync());
    }

    public boolean isPaymentOrderChainEnabled() {
        return Boolean.TRUE.equals(chains.getPaymentOrderComposition().getEnabled());
    }

    public boolean isPaymentOrderChainAsync() {
        return Boolean.TRUE.equals(chains.getPaymentOrderComposition().getAsync());
    }

    public BatchProductIngestionMode ingestionMode() {
        return BatchProductIngestionMode.builder()
            .functionGroupsMode(ingestionMode.getFunctionGroups())
            .dataGroupIngestionMode(ingestionMode.getDataGroups())
            .arrangementsMode(ingestionMode.getArrangements())
            .build();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (Boolean.TRUE.equals(chains.getTransactionComposition().getEnabled())
            && Boolean.TRUE.equals(chains.getTransactionManager().getEnabled())) {
            throw new InvalidPropertyException(this.getClass(), "backbase.stream.compositions.product.chains",
                "Either Transaction Composition or Transaction Manager should be enabled, never both");
        }
    }

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
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Chains {

        private TransactionManager transactionManager = new TransactionManager();
        private TransactionComposition transactionComposition = new TransactionComposition();
        private PaymentOrderComposition paymentOrderComposition = new PaymentOrderComposition();
    }

    @Data
    @NoArgsConstructor
    public static class IngestionMode {

        private BatchProductIngestionMode.FunctionGroupsMode functionGroups = BatchProductIngestionMode.FunctionGroupsMode.UPSERT;
        private BatchProductIngestionMode.DataGroupsMode dataGroups = BatchProductIngestionMode.DataGroupsMode.UPSERT;
        private BatchProductIngestionMode.ArrangementsMode arrangements = BatchProductIngestionMode.ArrangementsMode.UPSERT;
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static abstract class BaseComposition {

        private Boolean enabled = Boolean.FALSE;
        private Boolean async = Boolean.FALSE;
    }

    public static class TransactionManager extends TransactionComposition {

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class TransactionComposition extends BaseComposition {

        private List<String> excludeProductTypeExternalIds = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class PaymentOrderComposition extends BaseComposition {

        private List<String> excludeProductTypeExternalIds = new ArrayList<>();
    }
}
