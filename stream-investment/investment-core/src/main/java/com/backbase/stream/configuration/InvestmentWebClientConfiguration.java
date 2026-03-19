package com.backbase.stream.configuration;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * WebClient configuration for Investment service communication.
 *
 * <p>Provides optimized connection pooling, timeouts, and rate limiting to prevent:
 * <ul>
 *   <li>Resource exhaustion from too many concurrent connections</li>
 *   <li>503 Service Unavailable errors due to overwhelming the service</li>
 *   <li>Indefinite hangs from missing timeouts</li>
 * </ul>
 *
 * <p>All beans are explicitly named so they are unambiguous and never accidentally
 * replaced by Spring Boot auto-configuration or another generic bean of the same type.
 */
@Slf4j
@Configuration
public class InvestmentWebClientConfiguration {

    // Connection pool configuration constants.
    // MAX_CONNECTIONS=20 limits the number of open TCP connections to the Investment service,
    // preventing it from being overwhelmed (which causes 503 responses).
    // MAX_PENDING_ACQUIRES=100 bounds the in-memory queue so that callers receive a fast
    // failure rather than accumulating an unbounded backlog.
    private static final int MAX_CONNECTIONS = 20;
    private static final long MAX_IDLE_TIME_MINUTES = 5;
    private static final int MAX_PENDING_ACQUIRES = 100;
    private static final long PENDING_ACQUIRE_TIMEOUT_MILLIS = 45_000;

    // Timeout configuration constants (in seconds)
    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int READ_TIMEOUT_SECONDS = 30;
    private static final int WRITE_TIMEOUT_SECONDS = 30;

    /**
     * Dedicated {@link ConnectionProvider} for the Investment service client pool.
     *
     * <p>Using an explicit named bean (rather than {@code @ConditionalOnMissingBean ReactorResourceFactory})
     * avoids silently falling back to Spring Boot's auto-configured shared pool when one already exists
     * in the application context.
     *
     * @return investment-specific ConnectionProvider
     */
    @Bean("investmentConnectionProvider")
    public ConnectionProvider investmentConnectionProvider() {
        ConnectionProvider provider = ConnectionProvider.builder("investment-client-pool")
            .maxConnections(MAX_CONNECTIONS)
            .maxIdleTime(Duration.ofMinutes(MAX_IDLE_TIME_MINUTES))
            .maxLifeTime(Duration.ofMinutes(30))
            .pendingAcquireMaxCount(MAX_PENDING_ACQUIRES)
            .pendingAcquireTimeout(Duration.ofMillis(PENDING_ACQUIRE_TIMEOUT_MILLIS))
            .evictInBackground(Duration.ofSeconds(120))
            .build();

        log.info("Configured investment ConnectionProvider: maxConnections={}, maxIdleTime={}min,"
                + " pendingAcquireMaxCount={}",
            MAX_CONNECTIONS, MAX_IDLE_TIME_MINUTES, MAX_PENDING_ACQUIRES);

        return provider;
    }

    /**
     * Creates an {@link HttpClient} backed by the investment-specific connection pool and
     * with explicit connect / read / write timeouts.
     *
     * <p>This client ensures:
     * <ul>
     *   <li>Connection timeout prevents hanging on unresponsive servers</li>
     *   <li>Read/Write timeouts prevent indefinite hangs</li>
     *   <li>TCP_NODELAY enables immediate sending of small packets</li>
     *   <li>SO_KEEPALIVE maintains connection health for idle connections</li>
     * </ul>
     *
     * @param connectionProvider the investment-specific connection provider
     * @return configured HttpClient
     */
    @Bean("investmentHttpClient")
    public HttpClient investmentHttpClient(
        @Qualifier("investmentConnectionProvider") ConnectionProvider connectionProvider) {
        return HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_SECONDS * 1000)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true)
            .responseTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
            .doOnConnected(connection -> connection
                .addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)));
    }

}

