package com.backbase.stream.cursor.events;

import com.backbase.stream.cursor.model.IngestionCursor;
import com.backbase.stream.cursor.model.IngestionCursor.CursorSourceEnum;
import com.backbase.stream.cursor.model.PaymentCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.jms.annotation.JmsListener;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.FluxSink;

/**
 * Publish an Ingestion Cursor when a Payment Order is created in DBS
 */
@Slf4j
public class PaymentListener {

    private static final String VIRTUAL_TOPIC_PAYMENT_CREATED_EVENT =
        "VirtualTopic.com.backbase.paymentorder.persistence.event.spec.v1.PaymentOrderCreatedEvent";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final DirectProcessor<IngestionCursor> processor;
    private final FluxSink<IngestionCursor> sink;

    public PaymentListener() {
        this.processor = DirectProcessor.create();
        this.sink = processor.sink();
    }

    public Publisher<IngestionCursor> getPaymentCreatedEventCursorProcessor() {
        return processor;
    }

    @JmsListener(destination = VIRTUAL_TOPIC_PAYMENT_CREATED_EVENT)
    private void listenToAddedEvent(byte[] message) {
        try {
            PaymentCreatedEvent paymentEvent = objectMapper.readValue(message, PaymentCreatedEvent.class);
            IngestionCursor ingestionCursor = getIngestionCursor(
                paymentEvent.getPaymentOrder().getExternalUserId(),
                paymentEvent.getPaymentOrder().getCreatedAt(),
                CursorSourceEnum.PAYMENT_CREATED_EVENT,
                paymentEvent.getAdditions());
            log.info("Publishing Payment Event with payload: {}", ingestionCursor.toString());
            sink.next(ingestionCursor);
        } catch (Exception e) {
            log.error("Failed to create ingestion cursor from: {}", new String(message), e);
            sink.error(e);
        }
    }

    private IngestionCursor getIngestionCursor(String userId, Date createdAt,
        IngestionCursor.CursorSourceEnum source, Map<String, Object> additions) {
        IngestionCursor ingestionCursor = new IngestionCursor();
        ingestionCursor.setId(UUID.randomUUID());
        ingestionCursor.setCursorCreatedAt(OffsetDateTime.now());
        ingestionCursor.setExternalUserId(userId);
        ingestionCursor.setDateFrom(createdAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        ingestionCursor.setCursorState(IngestionCursor.CursorStateEnum.NOT_STARTED);
        ingestionCursor.setCursorSource(source);
        ingestionCursor.setAdditionalProperties(additions);
        return ingestionCursor;
    }
}
