package com.backbase.stream.compositions.product.core.mapper;

import com.backbase.stream.compositions.product.api.model.ArrangementIngestionConfig;
import com.backbase.stream.compositions.product.api.model.ArrangementsChainsConfig;
import com.backbase.stream.compositions.product.api.model.TransactionCompositionChainConfig;
import com.backbase.stream.compositions.product.core.model.RequestConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMapperTest {
    @Test
    void map() {
        ArrangementIngestionConfig config =
                new ArrangementIngestionConfig()
                        .chains(
                                new ArrangementsChainsConfig()
                                        .transactionComposition(
                                                new TransactionCompositionChainConfig()
                                                        .enabled(true)
                                                        .async(true)));

        ConfigMapper configMapper = new ConfigMapper();
        RequestConfig requestConfig = configMapper.map(config);

        assertEquals(true, requestConfig.getChains().getTransactionComposition().getEnabled());
        assertEquals(true, requestConfig.getChains().getTransactionComposition().getAsync());
    }

    @Test
    void mapNullTransactionComposition() {
        ArrangementIngestionConfig config =
                new ArrangementIngestionConfig()
                        .chains(
                                new ArrangementsChainsConfig());

        ConfigMapper configMapper = new ConfigMapper();
        RequestConfig requestConfig = configMapper.map(config);

        assertNull(requestConfig.getChains().getTransactionComposition());
    }

    @Test
    void mapNullChains() {
        ArrangementIngestionConfig config =
                new ArrangementIngestionConfig();

        ConfigMapper configMapper = new ConfigMapper();
        RequestConfig requestConfig = configMapper.map(config);

        assertNull(requestConfig.getChains());
    }

    @Test
    void mapNull() {
        ConfigMapper configMapper = new ConfigMapper();
        RequestConfig requestConfig = configMapper.map(null);

        assertNull(requestConfig);
    }
}
