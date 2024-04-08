package com.backbase.stream.context.events;

import static com.backbase.stream.context.config.ContextPropagationConfigurationProperties.TENANT_EVENT_HEADER_NAME;

import com.backbase.buildingblocks.backend.communication.context.OriginatorContext;
import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.scs.EventMessageProcessor;
import com.backbase.buildingblocks.backend.communication.event.scs.MessageFactoryProcessor;
import com.backbase.buildingblocks.persistence.model.Event;
import com.backbase.stream.context.ForwardedHeadersHolder;
import com.backbase.stream.context.config.ContextPropagationConfigurationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Propagates the ForwardedHeadersHolder when using spring-cloud-stream eventing.
 */
@Slf4j
@RequiredArgsConstructor
public class TenantEventMessageProcessor implements EventMessageProcessor, MessageFactoryProcessor {

    private final ContextPropagationConfigurationProperties properties;

    /**
     * Adds the currently-bound tenant ID to the TENANT_EVENT_HEADER_NAME header of the given {@link MessageBuilder}.
     */
    @Override
    public <T extends Event> void prepareEventMessage(MessageBuilder<T> messageBuilder,
        EnvelopedEvent<T> envelopedEvent) {

        prepareEventMessage(messageBuilder, envelopedEvent.getOriginatorContext());
    }

    /**
     * Adds the currently-bound tenant ID to the TENANT_EVENT_HEADER_NAME header of the given {@link MessageBuilder}.
     */
    @Override
    public <T> void prepareEventMessage(MessageBuilder<T> messageBuilder, OriginatorContext context) {
        var headers = ForwardedHeadersHolder.getValue();
        log.debug("prepareEventMessage {}", headers);
        if (headers != null) {
            var tenantId = headers.getFirst(properties.getTenantHttpHeaderName());
            if (tenantId != null) {
                messageBuilder.setHeader(TENANT_EVENT_HEADER_NAME, tenantId);
            } else {
                log.debug("A Tenant is not present in the context.");
            }
        }
    }

}
