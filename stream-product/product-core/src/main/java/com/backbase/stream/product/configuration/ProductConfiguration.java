package com.backbase.stream.product.configuration;


import com.backbase.dbs.arrangement.api.service.v3.ArrangementsApi;
import com.backbase.stream.product.service.ArrangementService;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Product  Configuration.
 */
@Configuration
@AllArgsConstructor
public class ProductConfiguration {

    @Bean
    public ArrangementService arrangementService(ArrangementsApi arrangementsApi,
        com.backbase.dbs.arrangement.api.integration.v3.ArrangementsApi arrangementsIntegrationApi) {
        return new ArrangementService(arrangementsApi, arrangementsIntegrationApi);
    }

}
