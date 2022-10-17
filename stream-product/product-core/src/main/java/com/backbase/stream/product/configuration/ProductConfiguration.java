package com.backbase.stream.product.configuration;


import com.backbase.dbs.arrangement.api.service.v2.ArrangementsApi;
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
    public ArrangementService arrangementService(ArrangementsApi arrangementsApi) {
        return new ArrangementService(arrangementsApi);
    }

}
