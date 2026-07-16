package com.backbase.stream.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the investment-caboose HTTP client connection pool and timeouts.
 *
 * <p>All values can be overridden via {@code application.yml} / {@code application.properties}
 * using the prefix {@code backbase.communication.services.investment-caboose.http-client}.
 *
 * <p>Example:
 * <pre>
 * backbase:
 *   communication:
 *     services:
 *       investment-caboose:
 *         http-client:
 *           max-connections: 20
 *           max-idle-time-minutes: 5
 *           max-pending-acquires: 100
 *           pending-acquire-timeout-millis: 45000
 *           connect-timeout-seconds: 10
 *           read-timeout-seconds: 30
 *           write-timeout-seconds: 30
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "backbase.communication.services.investment-caboose.http-client")
public class InvestmentCabooseWebClientProperties {

    /**
     * Maximum number of open TCP connections to investment-caboose.
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
     * Maximum number of requests that can wait for a free connection ({@code -1} = no queue limit).
     *
     * @see InvestmentWebClientProperties#getMaxPendingAcquires()
     */
    private int maxPendingAcquires = -1;

    /**
     * Maximum time (in milliseconds) a request will wait to acquire a connection from the pool.
     */
    private long pendingAcquireTimeoutMillis = 150_000;

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

