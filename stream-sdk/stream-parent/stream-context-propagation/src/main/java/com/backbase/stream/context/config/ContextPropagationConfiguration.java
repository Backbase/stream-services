package com.backbase.stream.context.config;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.scs.MessageInProcessor;
import com.backbase.stream.context.ForwardedHeadersAccessor;
import com.backbase.stream.context.events.TenantEventMessageProcessor;
import com.backbase.stream.context.events.TenantMessageInProcessor;
import com.backbase.stream.context.web.HeaderForwardingServerFilter;
import io.micrometer.context.ContextRegistry;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;


@AutoConfiguration
@EnableConfigurationProperties(ContextPropagationConfigurationProperties.class)
public class ContextPropagationConfiguration implements InitializingBean, DisposableBean {

    @Override
    public void afterPropertiesSet() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(new ForwardedHeadersAccessor());
        Hooks.enableAutomaticContextPropagation();
    }

    @Override
    public void destroy() {
        Hooks.disableAutomaticContextPropagation();
        ContextRegistry.getInstance().removeThreadLocalAccessor(ForwardedHeadersAccessor.KEY);
    }

    @Bean
    public HeaderForwardingServerFilter headerForwardingServerFilter(
        ContextPropagationConfigurationProperties properties) {
        return new HeaderForwardingServerFilter(properties);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(EnvelopedEvent.class)
    static class TenantEventConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public TenantEventMessageProcessor tenantEventMessageProcessor(
            ContextPropagationConfigurationProperties properties) {
            return new TenantEventMessageProcessor(properties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MessageInProcessor.class)
    static class TenantMessageInConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public TenantMessageInProcessor tenantMessageInProcessor(
            ContextPropagationConfigurationProperties properties) {
            return new TenantMessageInProcessor(properties);
        }
    }

}
