package com.backbase.stream.configuration;

import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.CustomerAccessGroupApi;
import java.util.List;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class CustomerAccessGroupCacheTestConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(new ConcurrentMapCache("customer-access-group")));
        return manager;
    }

    @Bean
    public CustomerAccessGroupApi mockCustomerAccessGroupApi() {
        return Mockito.mock(CustomerAccessGroupApi.class);
    }

}
