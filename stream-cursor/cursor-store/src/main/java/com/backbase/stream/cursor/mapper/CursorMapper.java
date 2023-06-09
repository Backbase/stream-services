package com.backbase.stream.cursor.mapper;

import com.backbase.stream.cursor.model.CursorItem;
import com.backbase.stream.cursor.model.IngestionCursor;
import org.mapstruct.Mapper;

@Mapper
public interface CursorMapper {

    CursorItem toCursorItem(IngestionCursor ingestionCursor);

    IngestionCursor toIngestionCursor(CursorItem cursorItem);
}
