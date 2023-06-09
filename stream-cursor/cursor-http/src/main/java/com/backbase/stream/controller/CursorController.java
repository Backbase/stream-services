package com.backbase.stream.controller;

import com.backbase.stream.cursor.CursorService;
import com.backbase.stream.cursor.api.CursorsApi;
import com.backbase.stream.cursor.model.IngestionCursor;
import java.util.UUID;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Rest implementation of Cursor API.
 */
@RestController
@AllArgsConstructor
public class CursorController implements CursorsApi {

    private final CursorService cursorService;

    /**
     * Create new Ingestion Cursor and store it in a database.
     *
     * @param ingestionCursor Ingestion Cursor to Create
     * @param exchange        Current HTTP Request
     * @return Create Ingestion Cursor
     */
    @Override
    public Mono<ResponseEntity<IngestionCursor>> createNewIngestionCursor(
        @Valid Mono<IngestionCursor> ingestionCursor, ServerWebExchange exchange) {
        return cursorService.create(ingestionCursor).map(ResponseEntity::ok);
    }

    /**
     * Delete all cursors. Only for development purposes.
     *
     * @param exchange Current HTTP Request
     * @return 200 on completion
     */
    @Override
    public Mono<ResponseEntity<Void>> deleteAllCursors(ServerWebExchange exchange) {
        return cursorService.deleteAllCursors().map(ResponseEntity::ok);
    }

    /**
     * Delete specific Cursor by ID.
     *
     * @param cursorId Cursor ID
     * @param exchange Current HTTP Request
     * @return 200 on completion
     */
    @Override
    public Mono<ResponseEntity<Void>> deleteCursorById(UUID cursorId, ServerWebExchange exchange) {
        return null;
    }

    /**
     * Get specific Cursor by ID.
     *
     * @param cursorId Cursor ID
     * @param exchange Current HTTP Request
     * @return Cursor ID. Empty mono if not found
     */
    @Override
    public Mono<ResponseEntity<IngestionCursor>> getCursorById(
        UUID cursorId, ServerWebExchange exchange) {
        return cursorService.findById(cursorId).map(ResponseEntity::ok);
    }

    /**
     * Return all ingestion cursors.
     *
     * @param exchange Current HTTP Request
     * @return List of Ingestion Cursors stores in cursor service
     */
    public Mono<ResponseEntity<Flux<IngestionCursor>>> getIngestionCursors(
        ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(cursorService.findAllCursors()));
    }

    /**
     * Update Cursor by ID.
     *
     * @param cursorId        Cursor ID
     * @param ingestionCursor THe new definition of the Ingestion Cursor
     * @param exchange        Current HTTP Request
     * @return 200 on completion
     */
    @Override
    public Mono<ResponseEntity<Void>> updateCursorById(
        UUID cursorId, @Valid Mono<IngestionCursor> ingestionCursor, ServerWebExchange exchange) {
        return cursorService.updateCursorById(cursorId, ingestionCursor).then().map(ResponseEntity::ok);
    }
}
