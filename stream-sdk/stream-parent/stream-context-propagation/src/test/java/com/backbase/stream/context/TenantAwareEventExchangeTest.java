package com.backbase.stream.context;


import static com.backbase.stream.context.config.ContextPropagationConfigurationProperties.TENANT_EVENT_HEADER_NAME;
import static com.backbase.stream.context.config.ContextPropagationConfigurationProperties.TENANT_HTTP_HEADER_NAME;
import static com.backbase.stream.context.reactor.HeaderForwardingContextSubscriber.FORWARDED_HEADERS_CONTEXT_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.config.EventBindableAutoConfiguration;
import com.backbase.buildingblocks.backend.communication.event.config.MessagingConfiguration;
import com.backbase.buildingblocks.backend.communication.event.config.SpringCloudStreamEventingAutoConfiguration;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.buildingblocks.persistence.model.Event;
import com.backbase.stream.context.TenantAwareEventExchangeTest.TestEventHandlerConfiguration;
import com.backbase.stream.context.config.ContextPropagationConfiguration;
import com.backbase.stream.context.events.TenantEventMessageProcessor;
import com.backbase.stream.context.events.TenantMessageInProcessor;
import lombok.Builder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.function.cloudevent.CloudEventsFunctionExtensionConfiguration;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.config.BindingServiceConfiguration;
import org.springframework.cloud.stream.function.FunctionConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;


@SpringJUnitConfig(classes = {
    PropertyPlaceholderAutoConfiguration.class,
    WebClientAutoConfiguration.class,
    BinderFactoryAutoConfiguration.class,
    BindingServiceConfiguration.class,
    CloudEventsFunctionExtensionConfiguration.class,
    ContextFunctionCatalogAutoConfiguration.class,
    FunctionConfiguration.class,
    SpringCloudStreamEventingAutoConfiguration.class,
    MessagingConfiguration.class,
    EventBindableAutoConfiguration.class,
    JacksonAutoConfiguration.class,
    ContextPropagationConfiguration.class,
    TestChannelBinderConfiguration.class,
    TestEventHandlerConfiguration.class
})
public class TenantAwareEventExchangeTest {

    @Autowired
    EventBus eventBus;

    @SpyBean
    TenantMessageInProcessor tenantMessageInProcessor;

    @SpyBean
    TenantEventMessageProcessor tenantEventMessageProcessor;

    @Test
    void tenantIdShouldBeSetInEventHeadersAndReactorHeaderContext() {
        TenantContext.setTenant("t1");
        var event = new EnvelopedEvent<TestEvent>();
        event.setEvent(TestEvent.builder().message("Test").build());
        eventBus.emitEvent(event);

        verify(tenantEventMessageProcessor).prepareEventMessage(any(), any(EnvelopedEvent.class));
        verify(tenantMessageInProcessor, timeout(2000))
            .processPostReceived(argThat(m -> "t1".equals(m.getHeaders().get(TENANT_EVENT_HEADER_NAME))), any(), any());
    }

    @Builder
    static class TestEvent extends Event {

        private String message;

        @Override
        public String toString() {
            return "TestEvent [message="
                + message
                + "]";
        }
    }

    @TestConfiguration
    static class TestEventHandlerConfiguration {

        @Bean
        public TestEventHandler testEventHandler() {
            return new TestEventHandler();
        }

        static class TestEventHandler implements EventHandler<TestEvent> {

            @Autowired
            WebClient.Builder webClientBuilder;

            @Override
            public void handle(EnvelopedEvent<TestEvent> internalRequest) {
                StepVerifier.create(webClientBuilder.build()
                        .get()
                        .uri("http://localhost")
                        .retrieve()
                        .toBodilessEntity())
                    .expectAccessibleContext()
                    .matches(c -> {
                        MultiValueMap<String, String> headers = c.get(FORWARDED_HEADERS_CONTEXT_KEY);
                        return "t1".equals(headers.get(TENANT_HTTP_HEADER_NAME).get(0));
                    })
                    .then()
                    .verifyError();
            }
        }
    }

}
