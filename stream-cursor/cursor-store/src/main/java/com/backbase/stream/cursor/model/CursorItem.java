package com.backbase.stream.cursor.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class CursorItem {

  @Id private UUID id;
  private OffsetDateTime cursorCreatedAt;
  private OffsetDateTime cursorModifiedAt;
  private IngestionCursor.CursorTypeEnum cursorType;
  private IngestionCursor.CursorStateEnum cursorState;
  private String cursorSource;
  private String recordUuid;
  private String internalLegalEntityId;
  private String externalLegalEntityId;
  private String externalUserId;
  private String internalUserId;
  private String arrangementId;
  private String externalArrangementId;
  private String bban;
  private String iban;
  private LocalDate dateFrom;
  private LocalDate dateTo;
  private OffsetDateTime dateTimeFrom;
  private OffsetDateTime dateTimeTo;
  private Map<String, Object> additionalProperties = null;
  /** State of the cursor. Newly created cursors start with NOT_STARTED state. */
  public enum CursorStateEnum {
    NOT_STARTED,
    ABANDONED,
    COMPLETED,
    FAILED,
    STARTED,
    STARTING,
    STOPPED,
    STOPPING,
    UNKNOWN;
  }

  public enum CursorTypeEnum {
    BATCH,
    REAL_TIME;
  }
}
