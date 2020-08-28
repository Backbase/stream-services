package com.backbase.stream.cursor.source;

import com.backbase.stream.cursor.model.IngestionCursor;
import com.backbase.stream.cursor.model.IngestionCursor.CursorSourceEnum;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Ingestion Cursor Source.
 */
@ConfigurationProperties("backbase.stream.cursor.source")
@Data
@NoArgsConstructor
public class CursorSourceProperties {

    /**
     * The Ingestion Cursor State to listen to.
     */
    private IngestionCursor.CursorStateEnum cursorState = IngestionCursor.CursorStateEnum.NOT_STARTED;

    /**
     * The Source of Cursor State Events to listen to.
     */
    private List<IngestionCursor.CursorSourceEnum> cursorSource = Arrays
        .asList(IngestionCursor.CursorSourceEnum.PAYMENT_CREATED_EVENT, IngestionCursor.CursorSourceEnum.LOGIN_EVENT);

    /**
     * The Type.
     */
    private IngestionCursor.CursorTypeEnum cursorType = IngestionCursor.CursorTypeEnum.REAL_TIME;

}
