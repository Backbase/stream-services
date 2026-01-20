package com.backbase.stream.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * Unit tests covering deterministic parts of the intraday price generator.
 *
 * <p>Focus is on {@link InvestmentIntradayAssetPriceService#generateIntradayOhlc(Double)} which is
 * deterministic given a provided Random seed; tests assert structural invariants and rounding.
 */
class InvestmentIntradayAssetPriceServiceTest {

    @Test
    void generateIntradayOhlc_shouldValidateInput() {
        assertThatThrownBy(() -> InvestmentIntradayAssetPriceService.generateIntradayOhlc(null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InvestmentIntradayAssetPriceService.generateIntradayOhlc(0.0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InvestmentIntradayAssetPriceService.generateIntradayOhlc(-1.0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RepeatedTest(10)
    void generateIntradayOhlc_shouldProduceValidOhlcStructure() {
        double previous = 100.0;
        Map<String, Double> ohlc = InvestmentIntradayAssetPriceService.generateIntradayOhlc(previous);

        assertThat(ohlc).containsKeys("open", "high", "low", "close");

        double open = ohlc.get("open");
        double high = ohlc.get("high");
        double low = ohlc.get("low");
        double close = ohlc.get("close");

        // Basic sanity
        assertThat(open).isGreaterThan(0.0);
        assertThat(high).isGreaterThan(0.0);
        assertThat(low).isGreaterThan(0.0);
        assertThat(close).isGreaterThan(0.0);

        // High must be >= max(open, close), low must be <= min(open, close)
        assertThat(high).isGreaterThanOrEqualTo(Math.max(open, close));
        assertThat(low).isLessThanOrEqualTo(Math.min(open, close));

        // Values should be rounded to 6 decimal places: value * 1e6 should be near-integer
        assertThat(Math.abs(Math.round(open * 1_000_000.0) - open * 1_000_000.0)).isLessThan(1e-6);
        assertThat(Math.abs(Math.round(high * 1_000_000.0) - high * 1_000_000.0)).isLessThan(1e-6);
        assertThat(Math.abs(Math.round(low * 1_000_000.0) - low * 1_000_000.0)).isLessThan(1e-6);
        assertThat(Math.abs(Math.round(close * 1_000_000.0) - close * 1_000_000.0)).isLessThan(1e-6);
    }
}