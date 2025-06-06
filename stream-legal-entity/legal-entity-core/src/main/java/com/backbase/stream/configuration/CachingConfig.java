package com.backbase.stream.configuration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CachingConfig {

    public static final String CUSTOMER_ACCESS_GROUP_CACHE = "customerAccessGroupCache";

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(CUSTOMER_ACCESS_GROUP_CACHE);
    }

}
