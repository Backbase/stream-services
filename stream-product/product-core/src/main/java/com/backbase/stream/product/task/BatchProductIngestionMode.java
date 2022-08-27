package com.backbase.stream.product.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Batch product ingestion mode. Keeps settings for three main resources involved in the process:
 * - Function groups
 * - Data groups
 * - Arrangements
 * <p>
 * Ingestion mode for each of those can be configured separately.
 */
@Builder
@AllArgsConstructor
public class BatchProductIngestionMode {

    private FunctionGroupsMode functionGroupsMode = FunctionGroupsMode.UPSERT;
    private DataGroupsMode dataGroupIngestionMode = DataGroupsMode.UPSERT;
    private ArrangementsMode arrangementsMode = ArrangementsMode.UPSERT;

    /**
     * @return True, if function groups should be replaced (function groups not existing in batch will be REMOVED from DBS).
     * Otherwise, they should be just updated.
     */
    public boolean isFunctionGroupsReplaceEnabled() {
        return functionGroupsMode == FunctionGroupsMode.REPLACE;
    }

    /**
     * @return True, if data groups should be replaced (data group and data group items not existing in batch will be REMOVED from DBS).
     * Otherwise, they should be just updated.
     */
    public boolean isDataGroupsReplaceEnabled() {
        return dataGroupIngestionMode == DataGroupsMode.REPLACE;
    }

    /**
     * @return True, if arrangements should be replaced (arrangements not existing in batch will be REMOVED from DBS).
     * Otherwise, they should be just updated.
     */
    public boolean isArrangementsReplaceEnabled() {
        return arrangementsMode == ArrangementsMode.REPLACE;
    }

    /**
     * Preset BatchProductIngestionMode: all set to UPSERT.
     *
     * @return BatchProductIngestionMode
     */
    public static BatchProductIngestionMode upsert() {
        return BatchProductIngestionMode.builder()
                .functionGroupsMode(FunctionGroupsMode.UPSERT)
                .dataGroupIngestionMode(DataGroupsMode.UPSERT)
                .arrangementsMode(ArrangementsMode.UPSERT)
                .build();
    }

    /**
     * Preset BatchProductIngestionMode: all set to REPLACE.
     *
     * @return BatchProductIngestionMode
     */
    public static BatchProductIngestionMode replace() {
        return BatchProductIngestionMode.builder()
                .functionGroupsMode(FunctionGroupsMode.REPLACE)
                .dataGroupIngestionMode(DataGroupsMode.REPLACE)
                .arrangementsMode(ArrangementsMode.REPLACE)
                .build();
    }

    /**
     * UPSERT - Function groups will be INSERTED/UPDATED.
     * REPLACE - Function groups will be REPLACED.
     */
    public enum FunctionGroupsMode {
        UPSERT,
        REPLACE
    }

    /**
     * UPSERT - Data groups will be INSERTED/UPDATED.
     * REPLACE - Data groups will be REPLACED.
     */
    public enum DataGroupsMode {
        UPSERT,
        REPLACE
    }

    /**
     * UPSERT - Arrangements will be INSERTED/UPDATED.
     * REPLACE - Arrangements will be REPLACED.
     */
    public enum ArrangementsMode {
        UPSERT,
        REPLACE
    }
}
