package com.backbase.stream.compositions.legalentity.core.config;

import com.backbase.stream.product.task.BatchProductIngestionMode;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.compositions.legal-entity")
public class LegalEntityConfigurationProperties {

    private String integrationBaseUrl = "http://legal-entity-integration:9000";
    private Chains chains = new Chains();
    private Events events = new Events();
    private Cursor cursor = new Cursor();
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
        private String baseUrl = "http://legal-entity-cursor:9000";
    }

    @Data
    @NoArgsConstructor
    public static class Chains {
        private Boolean includeSubsidiaries = Boolean.FALSE;
        private ProductComposition productComposition = new ProductComposition();
    }

    @Data
    public static abstract class BaseComposition {
        private Boolean enabled = Boolean.FALSE;
        private String baseUrl = "http://localhost:9002/";
        private Boolean async = Boolean.FALSE;
    }

    @NoArgsConstructor
    public static class ProductComposition extends BaseComposition {

    }

    @Data
    @NoArgsConstructor
    public static class IngestionMode {
        private BatchProductIngestionMode.FunctionGroupsMode functionGroups = BatchProductIngestionMode.FunctionGroupsMode.UPSERT;
        private BatchProductIngestionMode.DataGroupsMode dataGroups = BatchProductIngestionMode.DataGroupsMode.UPSERT;
        private BatchProductIngestionMode.ArrangementsMode arrangements = BatchProductIngestionMode.ArrangementsMode.UPSERT;
    }

    public Boolean isCompletedEventEnabled() {
        return Boolean.TRUE.equals(events.getEnableCompleted());
    }

    public Boolean isFailedEventEnabled() {
        return Boolean.TRUE.equals(events.getEnableFailed());
    }

    public boolean isProductChainEnabled() {
        return Boolean.TRUE.equals(chains.getProductComposition().getEnabled());
    }

    public boolean isProductChainAsync() {
        return Boolean.TRUE.equals(chains.getProductComposition().getAsync());
    }

    public BatchProductIngestionMode ingestionMode() {
        return BatchProductIngestionMode.builder()
                .functionGroupsMode(ingestionMode.getFunctionGroups())
                .dataGroupIngestionMode(ingestionMode.getDataGroups())
                .arrangementsMode(ingestionMode.getArrangements())
                .build();
    }
}
