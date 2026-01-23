package com.backbase.stream.investment.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/**
 * Representation of an asset returned by the List Assets API when the response includes expanded
 * market and latest price details and custom fields.
 * Example JSON:
 * <pre>
 * {
 *   "uuid": "123e4567-e89b-12d3-a456-426614174000",
 *   "market": {
 *     "code": "NYSE",
 *     "name": "New York Stock Exchange",
 *     "is_open": true,
 *     "today_session_starts": "2024-01-01T09:30:00+00:00Z",
 *     "today_session_ends": "2024-01-01T16:00:00+00:00Z",
 *     "market_reopens": null
 *   },
 *   "latest_price": {
 *     "amount": 123.45,
 *     "datetime": "2024-01-01T15:30:00+00:00Z",
 *     "open_price": 120.00,
 *     "high_price": 125.00,
 *     "low_price": 119.50,
 *     "previous_close_price": 121.00
 *   }
 * }
 * </pre>
 *
 * @param uuid Asset identifier (mapped from JSON property `uuid`)
 * @param expandedMarket Expanded market details (mapped from JSON property `market`)
 * @param expandedLatestPrice Expanded latest price details (mapped from JSON property `latest_price`)
 */
public record AssetWithMarketAndLatestPrice(@JsonProperty("uuid") UUID uuid,
                                            @JsonProperty("market") ExpandedMarket expandedMarket,
                                            @JsonProperty("latest_price") ExpandedLatestPrice expandedLatestPrice) {

}
