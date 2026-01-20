package com.backbase.stream.investment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record LatestPrice(
    @JsonProperty("amount") Double amount,
    @JsonProperty("datetime") OffsetDateTime datetime,
    @JsonProperty("open_price") Double openPrice,
    @JsonProperty("high_price") Double highPrice,
    @JsonProperty("low_price") Double lowPrice,
    @JsonProperty("previous_close_price") Double previousClosePrice
) {

}
