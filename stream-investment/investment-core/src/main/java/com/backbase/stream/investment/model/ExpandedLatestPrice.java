package com.backbase.stream.investment.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/**
 * Representation of an expanded latest price returned by the List Assets API when the response is
 * requested with expansion of latest price details and custom fields.
 * Example JSON:
 * <pre>
 * "latest_price": {
 *   "amount": 123.45,
 *   "datetime": "2026-01-21T12:58:00.000000Z",
 *   "open_price": 120.00,
 *   "high_price": 125.00,
 *   "low_price": 119.50,
 *   "previous_close_price": 121.00
 * }
 * </pre>
 */
public record ExpandedLatestPrice(
    @JsonProperty("amount") Double amount,
    @JsonProperty("datetime") OffsetDateTime datetime,
    @JsonProperty("open_price") Double openPrice,
    @JsonProperty("high_price") Double highPrice,
    @JsonProperty("low_price") Double lowPrice,
    @JsonProperty("previous_close_price") Double previousClosePrice
) {

}
