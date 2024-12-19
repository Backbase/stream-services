package com.backbase.stream.cursor;

import com.backbase.stream.cursor.events.ArrangementListener;
import com.backbase.stream.cursor.events.AuditLoginEventListener;
import com.backbase.stream.cursor.events.LoginEventListener;
import com.backbase.stream.cursor.events.PaymentListener;
import com.backbase.stream.cursor.model.IngestionCursor;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/** Collect all cursor sources and merge them in a single flux to be published. */
@Slf4j
@AllArgsConstructor
public class CursorStreamService {

  private final LoginEventListener loginEventListener;
  private final AuditLoginEventListener auditLoginEventListener;
  private final ArrangementListener arrangementAddedListener;
  private final PaymentListener paymentListener;

  /**
   * Publishes all ingestion cursor sources from Stream base Services.
   *
   * @param source The type of Cursor Source
   * @param state The State of Cursor to subscribe
   * @return Stream of Ingestion Cursors as they happen.
   */
  public Flux<IngestionCursor> findAllCursors(
      @Valid List<IngestionCursor.CursorSourceEnum> source,
      @Valid IngestionCursor.CursorStateEnum state) {

    return Flux.from(arrangementAddedListener.getArrangementUpdatedEventCursorProcessor())
        .mergeWith(arrangementAddedListener.getArrangementAddedEventCursorProcessor())
        .mergeWith(paymentListener.getPaymentCreatedEventCursorProcessor())
        .mergeWith(loginEventListener.getLoginEventProcessor())
        .mergeWith(auditLoginEventListener.getLoginEventProcessor())
        .filter(
            cursor -> {
              boolean matchesSource = source == null || source.contains(cursor.getCursorSource());
              boolean matchesState = state == null || state.equals(cursor.getCursorState());
              return matchesSource && matchesState;
            });
  }

  public Flux<IngestionCursor> findAllCursors(
      @Valid IngestionCursor.CursorSourceEnum source,
      @Valid IngestionCursor.CursorStateEnum state) {

    return Flux.from(arrangementAddedListener.getArrangementUpdatedEventCursorProcessor())
        .mergeWith(arrangementAddedListener.getArrangementAddedEventCursorProcessor())
        .mergeWith(paymentListener.getPaymentCreatedEventCursorProcessor())
        .mergeWith(loginEventListener.getLoginEventProcessor())
        .mergeWith(auditLoginEventListener.getLoginEventProcessor())
        .filter(
            cursor -> {
              boolean matchesSource = source == null || source.equals(cursor.getCursorSource());
              boolean matchesState = state == null || state.equals(cursor.getCursorState());
              return matchesSource && matchesState;
            });
  }
}
