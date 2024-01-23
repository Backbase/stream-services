package com.backbase.stream.compositions.product.core.config;

import com.backbase.audit.rest.spec.v3.model.AuditMessage;
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

    private LoginEvent loginEvent = new LoginEvent();
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
            ? isTransactionChainEnabled()
            : requestConfig.isTransactionChainEnabled().orElse(false);
    }

    public boolean isTransactionChainAsync(RequestConfig requestConfig) {
        return requestConfig == null || requestConfig.isTransactionChainAsync().isEmpty()
            ? isTransactionChainAsync()
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
    public static class LoginEvent {

        private Boolean enabled = Boolean.FALSE;
        private List<String> realms = List.of("retail");
        private String eventCategory = "Identity and Access";
        private String eventAction = "Attempt Login";
        private String objectType = "Authentication";
        private AuditMessage.Status status = AuditMessage.Status.SUCCESSFUL;

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

    @Getter
    @Setter
    public static class TransactionManager extends TransactionComposition {

        /**
         * The transaction manager supports a refresh call for all accounts at a single time;
         * however, some systems can't handle the throughput, so we enable one request per arrangement.
         */
        private Boolean splitPerArrangement = Boolean.TRUE;
        /**
         * In case a split per arrangement is made, and in sync mode, it limits the number of concurrent calls.
         */
        private Integer concurrency = 1;
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
