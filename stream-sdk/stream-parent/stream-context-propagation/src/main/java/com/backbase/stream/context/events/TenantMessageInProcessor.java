package com.backbase.stream.context.events;

import static com.backbase.stream.context.config.ContextPropagationConfigurationProperties.TENANT_EVENT_HEADER_NAME;

import com.backbase.buildingblocks.backend.communication.event.scs.MessageInProcessor;
import com.backbase.stream.context.TenantContext;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;

@Slf4j
public class TenantMessageInProcessor implements MessageInProcessor {

    /**
     * Extracts the tenant ID from the TENANT_EVENT_HEADER_NAME header of the given {@link Message} and uses it to bind
     * the appropriate Tenant to the current thread before the message consumer processing.
     * <p>
     * If the TENANT_EVENT_HEADER_NAME header is missing, or if it contains an invalid tenant ID, a warning is logged
     * and the event processing will be halted to guard against information leaking outside of a tenant context.
     * </p>
     */
    @Override
    public <T> void processPreReceived(Message<T> message, String channelName, String messageType) {
        String tenantId = message.getHeaders().get(TENANT_EVENT_HEADER_NAME, String.class);
        log.debug("filterReceivedEvent {}", tenantId);
        Optional<String> tenant = Optional.ofNullable(tenantId);
        if (tenant.isPresent()) {
            TenantContext.setTenant(tenant.get());
        } else {
            log.debug("Could not identify Tenant {}. ", this);
        }
    }

    /**
     * Clears the tenant context again after the event has been processed.
     */
    @Override
    public <T> void processPostReceived(Message<T> message, String channelName, String messageType) {
        log.debug("leaveContext {}", TenantContext.getTenant());
        TenantContext.clear();
        log.trace("TenantContext cleared");
    }
}
