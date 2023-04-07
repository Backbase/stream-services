package com.backbase.stream.cursor.configuration;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Cursor Source Configuration. */
@ConfigurationProperties("backbase.stream.events")
@Data
public class CursorServiceConfigurationProperties {

    /** Publish Event for each Arrangement logged in User is entitled for. */
    private boolean publishEntitledArrangements = true;
}
