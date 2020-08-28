package com.backbase.stream.cursor.source;

import com.backbase.stream.cursor.CursorStreamService;
import com.backbase.stream.cursor.model.IngestionCursor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;

/**
 * Message Producer that subscribes to Ingestion Cursors and publishes them on a Cloud Stream Output Channel.
 */
@AllArgsConstructor
@Slf4j
public class CursorMessageProducer extends MessageProducerSupport {

    private final CursorStreamService cursorStreamService;
    private final CursorSourceProperties cursorSourceProperties;

    @Override
    protected void doStart() {

        cursorStreamService.findAllCursors(
            cursorSourceProperties.getCursorSource(),
            cursorSourceProperties.getCursorState())
            .flatMap(ingestionCursor -> updateCursorState(ingestionCursor, IngestionCursor.CursorStateEnum.STARTING))
            .map(MessageBuilder::withPayload)
            .map(MessageBuilder::build)
            .subscribe(this::sendMessage);
    }

    private Mono<IngestionCursor> updateCursorState(IngestionCursor ingestionCursor, IngestionCursor.CursorStateEnum state) {
        ingestionCursor.setCursorState(state);
        return Mono.just(ingestionCursor);
    }

}
