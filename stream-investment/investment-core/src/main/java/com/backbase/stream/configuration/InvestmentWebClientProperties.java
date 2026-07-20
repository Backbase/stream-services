package com.backbase.stream.configuration;

import lombok.Data;

/**
 * Configuration properties for the Investment service HTTP client connection pool and timeouts.
 *
 * <p>Bound via {@code backbase.communication.services.investment.http-client} and
 * {@code backbase.communication.services.investment-caboose.http-client}.
 *
 * <p>Example (values shown are illustrative; defaults are in field JavaDoc below):
 * <pre>
 * backbase:
 *   communication:
 *     services:
 *       investment:
 *         http-client:
 *           max-connections: 50
 *           max-idle-time-minutes: 5
 *           max-life-time-minutes: 30
 *           max-pending-acquires: -1
 *           pending-acquire-timeout-millis: 90000
 *           evict-in-background-seconds: 120
 *           connect-timeout-seconds: 10
 *           read-timeout-seconds: 30
 *           write-timeout-seconds: 30
 *       investment-caboose:
 *         http-client:
 *           max-connections: 50
 * </pre>
 */
@Data
public class InvestmentWebClientProperties {

    /**
     * Maximum number of open TCP connections to the Investment service.
     * Limiting this prevents the service from being overwhelmed (which causes 503 responses).
     */
    private int maxConnections = 50;

    /**
     * Maximum time (in minutes) that a connection can remain idle in the pool before being evicted.
     */
    private long maxIdleTimeMinutes = 5;

    /**
     * Maximum lifetime (in minutes) of a pooled connection regardless of activity.
     */
    private long maxLifeTimeMinutes = 30;

    /**
     * Maximum number of requests that can wait for a free connection.
     *
     * <p>{@code -1} means no queue limit — callers block up to
     * {@link #pendingAcquireTimeoutMillis} instead of failing fast with
     * "Pending acquire queue has reached its maximum size".
     *
     * <p>Pair with application-level {@code flatMap} concurrency limits in the ingestion
     * services so the queue does not grow without bound.
     */
    private int maxPendingAcquires = -1;

    /**
     * Maximum time (in milliseconds) a request will wait to acquire a connection from the pool.
     *
     * <p>When all {@link #maxConnections} are in use, new requests wait here rather than
     * opening additional connections or failing immediately.
     */
    private long pendingAcquireTimeoutMillis = 90_000;

    /**
     * Background eviction interval (in seconds) for idle/expired connections in the pool.
     */
    private long evictInBackgroundSeconds = 120;

    /**
     * TCP connection timeout in seconds. Prevents hanging on unresponsive servers.
     */
    private int connectTimeoutSeconds = 10;

    /**
     * Read timeout in seconds. Prevents indefinite hangs waiting for a response.
     */
    private int readTimeoutSeconds = 30;

    /**
     * Write timeout in seconds. Prevents indefinite hangs while sending a request.
     */
    private int writeTimeoutSeconds = 30;
}

