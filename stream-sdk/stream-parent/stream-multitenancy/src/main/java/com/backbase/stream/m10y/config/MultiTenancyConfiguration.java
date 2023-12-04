package com.backbase.stream.m10y.config;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.scs.MessageInProcessor;
import com.backbase.stream.m10y.events.TenantEventMessageProcessor;
import com.backbase.stream.m10y.events.TenantMessageInProcessor;
import com.backbase.stream.m10y.reactor.TenantAwareContextSubscriberRegistrar;
import com.backbase.stream.m10y.web.HeaderForwardingServerFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@AutoConfiguration
@EnableConfigurationProperties(MultiTenancyConfigurationProperties.class)
public class MultiTenancyConfiguration {

    /**
     * Adds reactive server filter to chain.
     *
     * @param properties .
     * @return .
     */
    @Bean
    public HeaderForwardingServerFilter headerForwardingServerFilter(MultiTenancyConfigurationProperties properties) {
        return new HeaderForwardingServerFilter(properties);
    }

    @Configuration(proxyBeanMethods = false)
    static class TenantContextConfiguration {

        @Bean
        TenantAwareContextSubscriberRegistrar tenantReactorContextSubscriberRegistrar() {
            return new TenantAwareContextSubscriberRegistrar();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(EnvelopedEvent.class)
    static class TenantEventConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public TenantEventMessageProcessor tenantEventMessageProcessor() {
            return new TenantEventMessageProcessor();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MessageInProcessor.class)
    static class TenantMessageInConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public TenantMessageInProcessor tenantMessageInProcessor() {

            return new TenantMessageInProcessor();
        }
    }

}
