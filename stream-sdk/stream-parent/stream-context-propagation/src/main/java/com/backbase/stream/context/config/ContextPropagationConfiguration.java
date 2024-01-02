package com.backbase.stream.context.config;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.scs.MessageInProcessor;
import com.backbase.stream.context.events.TenantEventMessageProcessor;
import com.backbase.stream.context.events.TenantMessageInProcessor;
import com.backbase.stream.context.reactor.TenantAwareContextSubscriberRegistrar;
import com.backbase.stream.context.web.HeaderForwardingServerFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@AutoConfiguration
@EnableConfigurationProperties(ContextPropagationConfigurationProperties.class)
public class ContextPropagationConfiguration {

    /**
     * Adds reactive server filter to chain.
     *
     * @param properties .
     * @return .
     */
    @Bean
    public HeaderForwardingServerFilter headerForwardingServerFilter(
        ContextPropagationConfigurationProperties properties) {
        return new HeaderForwardingServerFilter(properties);
    }

    @Bean
    public TenantAwareContextSubscriberRegistrar tenantReactorContextSubscriberRegistrar() {
        return new TenantAwareContextSubscriberRegistrar();
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
