package com.backbase.stream.compositions.product.core.mapper;

import com.backbase.stream.compositions.product.api.model.*;
import com.backbase.stream.compositions.product.core.model.RequestConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigMapperTest {
    ConfigMapper configMapper = new ConfigMapper();

    @Test
    void map() {
        ArrangementIngestionConfig config =
                new ArrangementIngestionConfig()
                        .withChains(
                                new ArrangementsChainsConfig()
                                        .withTransactionComposition(
                                                new TransactionCompositionChainConfig()
                                                        .withEnabled(true)
                                                        .withAsync(true)));

        RequestConfig requestConfig = configMapper.map(config);

        assertEquals(true, requestConfig.getChains().getTransactionComposition().getEnabled());
        assertEquals(true, requestConfig.getChains().getTransactionComposition().getAsync());
    }

    @Test
    void mapNullTransactionComposition() {
        ArrangementIngestionConfig config =
                new ArrangementIngestionConfig()
                        .withChains(
                                new ArrangementsChainsConfig());

        RequestConfig requestConfig = configMapper.map(config);

        assertNull(requestConfig.getChains().getTransactionComposition());
    }

    @Test
    void mapNullChains() {
        ArrangementIngestionConfig config =
                new ArrangementIngestionConfig();

        RequestConfig requestConfig = configMapper.map(config);

        assertNull(requestConfig.getChains());
    }

    @Test
    void mapNull() {
        RequestConfig requestConfig = configMapper.map(null);

        assertNull(requestConfig);
    }

    @Test
    void mapProduct() {
        ProductIngestionConfig productConfig = getProductIngestionConfig();
        RequestConfig config = configMapper.mapProductIngestionConfig(productConfig);
        Assertions.assertNotNull(config);
    }

    @Test
    void mapNullProductIngestionConfig() {
        RequestConfig config = configMapper.mapProductIngestionConfig(null);
        Assertions.assertNull(config);
    }

    private ArrangementIngestionConfig getArrangementIngestionConfig() {
        ArrangementIngestionConfig config = new ArrangementIngestionConfig();
        ArrangementsChainsConfig chainsConfig = new ArrangementsChainsConfig();
        TransactionCompositionChainConfig transactionCompositionChainConfig = new TransactionCompositionChainConfig();
        transactionCompositionChainConfig.setAsync(false);
        transactionCompositionChainConfig.setEnabled(false);
        chainsConfig.setTransactionComposition(transactionCompositionChainConfig);
        config.setChains(chainsConfig);
        return config;
    }

    private ProductIngestionConfig getProductIngestionConfig() {
        ProductIngestionConfig config = new ProductIngestionConfig();
        ProductChainsConfig chainsConfig = new ProductChainsConfig();
        TransactionCompositionChainConfig transactionCompositionChainConfig = new TransactionCompositionChainConfig();
        transactionCompositionChainConfig.setAsync(false);
        transactionCompositionChainConfig.setEnabled(false);
        chainsConfig.setTransactionComposition(transactionCompositionChainConfig);
        PaymentOrderCompositionChainConfig paymentOrderCompositionChainConfig = new PaymentOrderCompositionChainConfig();
        paymentOrderCompositionChainConfig.setAsync(true);
        paymentOrderCompositionChainConfig.setEnabled(false);
        chainsConfig.setPaymentOrderComposition(paymentOrderCompositionChainConfig);
        config.setChains(chainsConfig);
        return config;
    }
}
