package com.backbase.stream.compositions.product.core.mapper;

import com.backbase.stream.compositions.product.api.model.ArrangementIngestionConfig;
import com.backbase.stream.compositions.product.api.model.ArrangementsChainsConfig;
import com.backbase.stream.compositions.product.api.model.PaymentOrderCompositionChainConfig;
import com.backbase.stream.compositions.product.api.model.TransactionCompositionChainConfig;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
public class ConfigMapper {
    ProductConfigurationProperties.Chains map(ArrangementIngestionConfig config) {
        if (config == null) {
            return null;
        }

        ArrangementsChainsConfig chainsConfig = config.getChains();

        return chainsConfig != null ?
                ProductConfigurationProperties.Chains
                        .builder()
                        .transactionComposition(this.map(config.getChains().getTransactionComposition()))
                        .paymentOrderComposition(
                                ProductConfigurationProperties.PaymentOrderComposition
                                        .builder()
                                        .enabled(chainsConfig.getPaymentOrderComposition().getEnabled())
                                        .build())
                        .build()
                : null;
    }

    private ProductConfigurationProperties.TransactionComposition map(TransactionCompositionChainConfig config) {
        return config != null ?
                ProductConfigurationProperties.TransactionComposition
                        .builder()
                        .enabled(config.getEnabled())
                        .build()
                : null;
    }

    private ProductConfigurationProperties.PaymentOrderComposition map(PaymentOrderCompositionChainConfig config) {
        return config != null ?
                ProductConfigurationProperties.PaymentOrderComposition
                        .builder()
                        .enabled(config.getEnabled())
                        .build()
                : null;
    }
}
