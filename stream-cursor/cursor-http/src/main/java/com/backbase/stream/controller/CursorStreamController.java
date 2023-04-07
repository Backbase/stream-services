package com.backbase.stream.controller;

import com.backbase.stream.cursor.CursorStreamService;
import com.backbase.stream.cursor.api.CursorStreamApi;
import com.backbase.stream.cursor.model.IngestionCursor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

/**
 * Reactive Cursor Stream Controller which allow subscribers to subscribe to a Ingestion Cursor
 * Stream.
 */
@RestController
@AllArgsConstructor
@Slf4j
public class CursorStreamController implements CursorStreamApi {

    private final CursorStreamService cursorStreamService;

    /**
     * Reactive HTTP Stream of ingestion cursor as they happen.
     *
     * @param source Source Filter
     * @param state State filter
     * @param exchange Current HTTP Request
     * @return Stream of Ingestion Cursors
     */
    @Override
    public Mono<ResponseEntity<Flux<IngestionCursor>>> getIngestionCursorStream(
            @Valid String source, @Valid String state, ServerWebExchange exchange) {
        IngestionCursor.CursorSourceEnum ingestionCursorSource =
                source != null ? IngestionCursor.CursorSourceEnum.fromValue(source) : null;
        IngestionCursor.CursorStateEnum ingestionCursorState =
                state != null ? IngestionCursor.CursorStateEnum.fromValue(state) : null;
        Flux<IngestionCursor> allCursors =
                cursorStreamService.findAllCursors(ingestionCursorSource, ingestionCursorState);
        return Mono.just(ResponseEntity.ok(allCursors));
    }
}
