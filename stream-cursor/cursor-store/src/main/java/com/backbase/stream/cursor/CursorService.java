package com.backbase.stream.cursor;

import com.backbase.stream.cursor.model.IngestionCursor;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * Reactive Cursor Service that stores cursors in a MongoDB.
 */
@SuppressWarnings("unused")
@Slf4j
@AllArgsConstructor
public class CursorService {

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    /**
     * Find by Unique Id.
     *
     * @param uuid uuid
     * @return Ingestion Cursor
     */
    public Mono<IngestionCursor> findById(UUID uuid) {
        return reactiveMongoTemplate.findById(uuid, IngestionCursor.class);
    }

    /**
     * Create new Ingestion Cursor.
     *
     * @param ingestionCursor Ingestion Cursor
     * @return Ingestion Cursor
     */
    public Mono<IngestionCursor> create(Mono<IngestionCursor> ingestionCursor) {
        return reactiveMongoTemplate.save(ingestionCursor);
    }

    /**
     * Update Ingestion Cursor.
     *
     * @param cursorId        The ID to update
     * @param ingestionCursor Ingestion Cursor
     * @return updated Ingestion Cursor
     */
    public Mono<IngestionCursor> updateCursorById(UUID cursorId, Mono<IngestionCursor> ingestionCursor) {
        return ingestionCursor.flatMap(cursor -> updateCursorById(cursorId, cursor))
            .doOnError(throwable -> log.error("Error while patching cursor: " + ingestionCursor));
    }

    private Mono<IngestionCursor> updateCursorById(UUID cursorId, IngestionCursor cursor) {
        return reactiveMongoTemplate.findById(cursorId, IngestionCursor.class)
            .map(ingestionCursor ->
                ingestionCursor
                    .cursorModifiedAt(OffsetDateTime.now())
                    .cursorState(cursor.getCursorState())
            );
    }

    /**
     * Reset all Ingestion Cursors. Dangerous!!
     *
     * @return Mono on completion
     */
    public Mono<Void> deleteAllCursors() {
        return reactiveMongoTemplate.dropCollection(IngestionCursor.class);
    }

    /**
     * Return list of all ingestion cursors.
     *
     * @return List of Ingestion Cursors
     */
    public Flux<IngestionCursor> findAllCursors() {
        return reactiveMongoTemplate.findAll(IngestionCursor.class);
    }
}