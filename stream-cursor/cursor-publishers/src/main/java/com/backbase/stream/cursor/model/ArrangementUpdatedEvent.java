package com.backbase.stream.cursor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Arrangement Updated Event.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArrangementUpdatedEvent extends AbstractDbsEvent {

    private String arrangementPutId;
}
