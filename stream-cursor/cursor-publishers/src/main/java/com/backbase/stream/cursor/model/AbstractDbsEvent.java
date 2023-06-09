package com.backbase.stream.cursor.model;

import java.io.Serializable;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Abstract DBS Event.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractDbsEvent implements Serializable {

    private Map<String, Object> additions;
    private String version;
}
