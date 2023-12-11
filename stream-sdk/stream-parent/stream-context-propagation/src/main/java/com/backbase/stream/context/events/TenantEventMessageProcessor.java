package com.backbase.stream.context.events;

import com.backbase.buildingblocks.backend.communication.context.OriginatorContext;
import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.scs.EventMessageProcessor;
import com.backbase.buildingblocks.backend.communication.event.scs.MessageFactoryProcessor;
import com.backbase.buildingblocks.persistence.model.Event;
import com.backbase.stream.context.TenantContext;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Propagates the TenantContext when using spring-cloud-stream eventing.
 */
@Slf4j
public class TenantEventMessageProcessor implements EventMessageProcessor, MessageFactoryProcessor {

    public static final String TID_HEADER_NAME = "bbTenantId";

    /**
     * Adds the currently-bound tenant ID to the {@link #TID_HEADER_NAME} header of the given {@link MessageBuilder}.
     */
    @Override
    public <T extends Event> void prepareEventMessage(MessageBuilder<T> messageBuilder,
        EnvelopedEvent<T> envelopedEvent) {

        prepareEventMessage(messageBuilder, envelopedEvent.getOriginatorContext());
    }

    /**
     * Adds the currently-bound tenant ID to the {@link #TID_HEADER_NAME} header of the given {@link MessageBuilder}.
     */
    @Override
    public <T> void prepareEventMessage(MessageBuilder<T> messageBuilder, OriginatorContext context) {
        Optional<String> tenant = TenantContext.getTenant();
        log.debug("prepareEventMessage {}", tenant);
        if (tenant.isPresent()) {
            messageBuilder.setHeader(TID_HEADER_NAME, tenant.get());
        } else {
            log.debug("A Tenant is not present in the context.");
        }
    }

}
