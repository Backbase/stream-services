package com.backbase.stream.investment.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/**
 * Representation of an expanded market returned by the List Assets API when the response is
 * requested with expansion of market details and custom fields.
 * Example JSON:
 * <pre>
 *"market": {
 *            "code": "XETR",
 *            "name": "XETRA",
 *            "is_open": true,
 *            "today_session_starts": "2026-01-22T09:00:00.000000Z",
 *            "today_session_ends": "2026-01-22T17:30:00.000000Z",
 *            "market_reopens": "2026-01-23T09:00:00.000000Z"
 *          }
 * </pre>
 *
 */
public record ExpandedMarket(@JsonProperty("code") String code,
                             @JsonProperty("name") String name,
                             @JsonProperty("is_open") Boolean isOpen,
                             @JsonProperty("today_session_starts") OffsetDateTime todaySessionStarts,
                             @JsonProperty("today_session_ends") OffsetDateTime todaySessionEnds,
                             @JsonProperty("market_reopens") OffsetDateTime marketReopens) {

}
