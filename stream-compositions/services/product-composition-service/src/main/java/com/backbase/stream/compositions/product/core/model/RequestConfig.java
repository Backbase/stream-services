package com.backbase.stream.compositions.product.core.model;

import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Setter
@Getter
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

    public Optional<Boolean> isTransactionChainAsync() {
        return chains == null
                || chains.getTransactionComposition() == null
                || chains.getTransactionComposition().getAsync() == null
                ? Optional.empty()
                : Optional.of(Boolean.TRUE.equals(chains.getTransactionComposition().getAsync()));
    }
    public Optional<Boolean> isPaymentOrderChainEnabled() {
        return chains == null
                || chains.getPaymentOrderComposition() == null
                || chains.getPaymentOrderComposition().getEnabled() == null
                ? Optional.empty()
                : Optional.of(Boolean.TRUE.equals(chains.getPaymentOrderComposition().getEnabled()));
    }

    public Optional<Boolean> isPaymentOrderChainAsync() {
        return chains == null
                || chains.getPaymentOrderComposition() == null
                || chains.getPaymentOrderComposition().getAsync() == null
                ? Optional.empty()
                : Optional.of(Boolean.TRUE.equals(chains.getPaymentOrderComposition().getAsync()));
    }


    @Setter
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Chains {
        private ProductConfigurationProperties.TransactionComposition transactionComposition;
        private ProductConfigurationProperties.PaymentOrderComposition paymentOrderComposition;
    }

    @Setter
    @Getter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static abstract class BaseComposition {
        private Boolean enabled = Boolean.FALSE;
        private Boolean async = Boolean.FALSE;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class TransactionComposition extends ProductConfigurationProperties.BaseComposition {
        private List<String> excludeProductTypeExternalIds = new ArrayList<>();
    }


    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class PaymentOrderComposition extends ProductConfigurationProperties.BaseComposition {
        private List<String> excludeProductTypeExternalIds = new ArrayList<>();
    }
}
