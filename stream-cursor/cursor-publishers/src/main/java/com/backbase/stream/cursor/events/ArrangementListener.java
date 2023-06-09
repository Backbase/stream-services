package com.backbase.stream.cursor.events;

import com.backbase.stream.cursor.model.ArrangementAddedEvent;
import com.backbase.stream.cursor.model.ArrangementUpdatedEvent;
import com.backbase.stream.cursor.model.IngestionCursor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.jms.annotation.JmsListener;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.FluxSink;

/**
 * Publish an Ingestion Cursor when an Arrangement is created or updated in DBS. Useful for ingestion of transactions
 * when arrangement is created.
 */
@Slf4j
public class ArrangementListener {

    private static final String VIRTUAL_TOPIC_ARRANGEMENT_ADDED_EVENT =
        "VirtualTopic.com.backbase.pandp.arrangement.event.spec.v1.ArrangementAddedEvent";
    private static final String VIRUTAL_TOPIC_ARRANGEMENT_UPDATED_EVENT =
        "VirtualTopic.com.backbase.pandp.arrangement.event.spec.v1.ArrangementUpdatedEvent";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final DirectProcessor<IngestionCursor> addedProcessor;
    private final FluxSink<IngestionCursor> addedSink;
    private final DirectProcessor<IngestionCursor> updatedProcessor;
    private final FluxSink<IngestionCursor> updatedSink;

    public ArrangementListener() {
        this.addedProcessor = DirectProcessor.create();
        this.updatedProcessor = DirectProcessor.create();

        this.addedSink = addedProcessor.sink();
        this.updatedSink = updatedProcessor.sink();
    }

    public Publisher<IngestionCursor> getArrangementAddedEventCursorProcessor() {
        return addedProcessor;
    }

    public Publisher<IngestionCursor> getArrangementUpdatedEventCursorProcessor() {
        return updatedProcessor;
    }

    @JmsListener(destination = VIRTUAL_TOPIC_ARRANGEMENT_ADDED_EVENT)
    private void listenToAddedEvent(byte[] message) {
        try {
            ArrangementAddedEvent arrangementEvent =
                objectMapper.readValue(message, ArrangementAddedEvent.class);
            IngestionCursor ingestionCursor =
                getIngestionCursor(
                    arrangementEvent.getArrangementPostId(),
                    IngestionCursor.CursorSourceEnum.ARRANGEMENT_ADDED_EVENT,
                    arrangementEvent.getAdditions());
            addedSink.next(ingestionCursor);
        } catch (Exception e) {
            log.error("Failed to create ingestion cursor from: {}", new String(message), e);
            addedSink.error(e);
        }
    }

    @JmsListener(destination = VIRUTAL_TOPIC_ARRANGEMENT_UPDATED_EVENT)
    private void listenToUpdatedEvent(byte[] message) {
        try {
            ArrangementUpdatedEvent arrangementEvent =
                objectMapper.readValue(message, ArrangementUpdatedEvent.class);
            IngestionCursor ingestionCursor =
                getIngestionCursor(
                    arrangementEvent.getArrangementPutId(),
                    IngestionCursor.CursorSourceEnum.ARRANGEMENT_UPDATED_EVENT,
                    arrangementEvent.getAdditions());
            updatedSink.next(ingestionCursor);
        } catch (Exception e) {
            log.error("Failed to create ingestion cursor from: {}", new String(message), e);
            updatedSink.error(e);
        }
    }

    private IngestionCursor getIngestionCursor(
        String arrangementId,
        IngestionCursor.CursorSourceEnum source,
        Map<String, Object> additions) {
        IngestionCursor ingestionCursor = new IngestionCursor();
        ingestionCursor.setId(UUID.randomUUID());
        ingestionCursor.setCursorCreatedAt(OffsetDateTime.now());
        ingestionCursor.setExternalArrangementId(arrangementId);
        ingestionCursor.setCursorState(IngestionCursor.CursorStateEnum.NOT_STARTED);
        ingestionCursor.setCursorSource(source);
        ingestionCursor.setAdditionalProperties(additions);
        return ingestionCursor;
    }
}
