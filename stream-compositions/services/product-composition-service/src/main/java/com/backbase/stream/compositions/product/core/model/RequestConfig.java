package com.backbase.stream.compositions.product.core.model;

import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestConfig {
    private Chains chains;

    public Optional<Boolean> isTransactionChainEnabled() {
        return chains == null
                || chains.getTransactionComposition() == null
                || chains.getTransactionComposition().getEnabled() == null
                ? Optional.empty()
                : Optional.of(Boolean.TRUE.equals(chains.getTransactionComposition().getEnabled()));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Chains {
        private ProductConfigurationProperties.TransactionComposition transactionComposition;
        private ProductConfigurationProperties.PaymentOrderComposition paymentOrderComposition;
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static abstract class BaseComposition {
        private Boolean enabled = Boolean.FALSE;
        private Boolean async = Boolean.FALSE;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class TransactionComposition extends ProductConfigurationProperties.BaseComposition {
        private List<String> excludeProductTypeExternalIds = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class PaymentOrderComposition extends ProductConfigurationProperties.BaseComposition {
        private List<String> excludeProductTypeExternalIds = new ArrayList<>();
    }
}
