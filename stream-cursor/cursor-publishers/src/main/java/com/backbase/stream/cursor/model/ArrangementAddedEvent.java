package com.backbase.stream.cursor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Arrangement Added Event. {"arrangementPostId":"WXQC175","internalId":"8a6d80e86fcd163c016fefbb3fd1001b","version":0}
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArrangementAddedEvent extends AbstractDbsEvent {

    private String arrangementPostId;
    private String internalId;
}
