package com.backbase.stream.transaction.utils;


import com.backbase.dbs.transaction.api.service.v2.model.Currency;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.experimental.UtilityClass;

/**
 * Utility class for generating transactions.
 */
@UtilityClass
public class CommonHelpers {

    /**
     * Generate random number in range.
     *
     * @param min lower bound.
     * @param max upper bound.
     * @return Random number
     */
    public static int generateRandomNumberInRange(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        return ThreadLocalRandom.current().nextInt((max - min) + 1) + min;
    }

    /**
     * Generate random currency amount.
     *
     * @param currencyCode Currency Code.
     * @param min          Lower Bound
     * @param max          Upper Bound
     * @return Random amount with Currency Code.
     */
    public static Currency generateRandomAmountInRange(String currencyCode, long min, long max) {
        long clamp = max * 10 - min * 10;
        long value = Math.abs((ThreadLocalRandom.current().nextLong() % clamp));
        String amount = new BigDecimal("" + ((value / 10D) + min)).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
        return new Currency()
            .amount(amount)
            .currencyCode(currencyCode);
    }

    public static <T> T getRandomFromList(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    public static <T extends Enum> T getRandomFromEnumValues(T[] values) {
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }

}
