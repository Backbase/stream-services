package com.backbase.stream.compositions.product.core.mapper;

import com.backbase.stream.compositions.product.api.model.ArrangementIngestionConfig;
import com.backbase.stream.compositions.product.api.model.ArrangementsChainsConfig;
import com.backbase.stream.compositions.product.api.model.TransactionCompositionChainConfig;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import com.backbase.stream.compositions.product.core.model.RequestConfig;
import org.springframework.stereotype.Component;

@Component
public class ConfigMapper {
    public RequestConfig map(ArrangementIngestionConfig config) {
        return config != null ?
                RequestConfig
                        .builder()
                        .chains(this.map(config.getChains()))
                        .build()
                : null;
    }

    private RequestConfig.Chains map(ArrangementsChainsConfig chainsConfig) {
        return chainsConfig != null ?
                RequestConfig.Chains
                        .builder()
                        .transactionComposition(this.map(chainsConfig.getTransactionComposition()))
                        .build()
                : null;
    }

    private ProductConfigurationProperties.TransactionComposition map(TransactionCompositionChainConfig config) {
        return config != null ?
                ProductConfigurationProperties.TransactionComposition
                        .builder()
                        .enabled(config.getEnabled())
                        .async(config.getAsync())
                        .build()
                : null;
    }
}
