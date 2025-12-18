package com.backbase.stream.investment;

/**
 * Define pseudorandomly chosen double value between the specified.
 *
 * @param origin - the least value that can be returned
 * @param bound  - the upper bound (exclusive) for the returned value
 */
public record RandomParam(double origin, double bound) {

}
