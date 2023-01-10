package com.backbase.stream.compositions.product.core.mapper;

import com.backbase.stream.compositions.product.api.model.ArrangementIngestionConfig;
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


    private ArrangementIngestionConfig getArrangementIngestionConfig() {

        return JsonUtil.readJsonFileToObject(ArrangementIngestionConfig.class, "integration-data/arrangmentIngestConfig.json");
    }


}