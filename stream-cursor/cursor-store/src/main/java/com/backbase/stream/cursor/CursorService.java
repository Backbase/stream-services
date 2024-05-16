package com.backbase.stream.cursor;

import com.backbase.stream.cursor.configuration.CursorRepository;
import com.backbase.stream.cursor.mapper.CursorMapper;
import com.backbase.stream.cursor.model.IngestionCursor;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@SuppressWarnings("unused")
@Slf4j
@AllArgsConstructor
public class CursorService {

    private final CursorRepository cursorRepository;
    private final CursorMapper cursorMapper = Mappers.getMapper(CursorMapper.class);

    /**
     * Find by Unique Id.
     *
     * @param uuid uuid
     * @return Ingestion Cursor
     */
    public Mono<IngestionCursor> findById(UUID uuid) {
        return cursorRepository.findById(uuid.toString()).map(cursorMapper::toIngestionCursor);
    }

    /**
     * Create new Ingestion Cursor.
     *
     * @param ingestionCursor Ingestion Cursor
     * @return Ingestion Cursor
     */
    public Mono<IngestionCursor> create(Mono<IngestionCursor> ingestionCursor) {
        return ingestionCursor.map(cursorMapper::toCursorItem)
            .flatMap(cursorRepository::save)
            .map(cursorMapper::toIngestionCursor);
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
        return Mono.just(cursor).map(ingestionCursor -> cursor.id(cursorId).cursorModifiedAt(OffsetDateTime.now()))
            .map(cursorMapper::toCursorItem)
            .flatMap(cursorRepository::save)
            .map(cursorMapper::toIngestionCursor);
    }

    /**
     * Reset all Ingestion Cursors. Dangerous!!
     *
     * @return Mono on completion
     */
    public Mono<Void> deleteAllCursors() {
        return cursorRepository.deleteAll();
    }

    /**
     * Return list of all ingestion cursors.
     *
     * @return List of Ingestion Cursors
     */
    public Flux<IngestionCursor> findAllCursors() {
        return cursorRepository.findAll().map(cursorMapper::toIngestionCursor);
    }
}
