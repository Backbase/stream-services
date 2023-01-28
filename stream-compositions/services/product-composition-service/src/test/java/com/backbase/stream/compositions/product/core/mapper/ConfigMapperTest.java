package com.backbase.stream.compositions.product.core.mapper;

import com.backbase.stream.compositions.product.api.model.ArrangementIngestionConfig;
import com.backbase.stream.compositions.product.api.model.ArrangementsChainsConfig;
import com.backbase.stream.compositions.product.api.model.TransactionCompositionChainConfig;
import com.backbase.stream.compositions.product.api.model.PaymentOrderCompositionChainConfig;
import com.backbase.stream.compositions.product.api.model.ProductChainsConfig;
import com.backbase.stream.compositions.product.api.model.ProductIngestionConfig;
import com.backbase.stream.compositions.product.core.model.RequestConfig;
import com.backbase.stream.compositions.product.util.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class ConfigMapperTest {

    ConfigMapper configMapper = new ConfigMapper();

    @Test
    void map() {
        ArrangementIngestionConfig arrangementConfig = getArrangementIngestionConfig();
        RequestConfig config = configMapper.map(arrangementConfig);
        Assertions.assertNotNull(config);
    }

    @Test
    void mapNullArrangmentIngestionConfig() {
        RequestConfig config = configMapper.map(null);
        Assertions.assertNull(config);
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