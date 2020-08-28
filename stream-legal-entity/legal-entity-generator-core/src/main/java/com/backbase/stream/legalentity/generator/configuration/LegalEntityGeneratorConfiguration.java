package com.backbase.stream.legalentity.generator.configuration;

import com.backbase.stream.legalentity.generator.LegalEntityGenerator;
import com.backbase.stream.product.generator.ProductGenerator;
import com.backbase.stream.product.generator.configuration.ProductGeneratorConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableConfigurationProperties({LegalEntityGeneratorConfigurationProperties.class})
@Import(ProductGeneratorConfiguration.class)
public class LegalEntityGeneratorConfiguration {

    @Bean
    public LegalEntityGenerator legalEntityGenerator(LegalEntityGeneratorConfigurationProperties configurationProperties,
                                                     ProductGenerator productGenerator) {
        return new LegalEntityGenerator(configurationProperties, productGenerator);
    }

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }


}
