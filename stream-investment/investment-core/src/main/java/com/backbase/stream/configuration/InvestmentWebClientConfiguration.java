package com.backbase.stream.configuration;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorResourceFactory;
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
 */
@Slf4j
@Configuration
public class InvestmentWebClientConfiguration {

    // Connection pool configuration constants
    private static final int MAX_CONNECTIONS = 100;
    private static final long MAX_IDLE_TIME_MINUTES = 5;
    private static final int MAX_PENDING_ACQUISITIONS = 1000;
    private static final long PENDING_ACQUISITION_TIMEOUT_MILLIS = 45000;

    // Timeout configuration constants (in seconds)
    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int READ_TIMEOUT_SECONDS = 30;
    private static final int WRITE_TIMEOUT_SECONDS = 30;

    /**
     * Configures a ReactorResourceFactory for optimized connection pooling and resource management.
     *
     * <p>This bean ensures:
     * <ul>
     *   <li>Connection pool doesn't exceed MAX_CONNECTIONS to prevent overwhelming downstream services</li>
     *   <li>Idle connections are released after MAX_IDLE_TIME_MINUTES</li>
     *   <li>Pending acquisition queue is limited to prevent unbounded request queuing</li>
     *   <li>Connection acquisition timeout prevents indefinite waits</li>
     * </ul>
     *
     * @return configured ReactorResourceFactory
     */
    @Bean
    @ConditionalOnMissingBean
    public ReactorResourceFactory reactorResourceFactory() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("investment-client-pool")
            .maxConnections(MAX_CONNECTIONS)
            .maxIdleTime(java.time.Duration.ofMinutes(MAX_IDLE_TIME_MINUTES))
            .maxLifeTime(java.time.Duration.ofMinutes(30))
            .pendingAcquisitionMaxCount(MAX_PENDING_ACQUISITIONS)
            .pendingAcquisitionTimeout(
                java.time.Duration.ofMillis(PENDING_ACQUISITION_TIMEOUT_MILLIS))
            .evictInBackground(java.time.Duration.ofSeconds(120))
            .build();

        ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
        resourceFactory.setConnectionProvider(connectionProvider);

        log.info("Configured ReactorResourceFactory for Investment service with maxConnections={},"
                + " maxIdleTime={}min, pendingAcquisitionMaxCount={}",
            MAX_CONNECTIONS, MAX_IDLE_TIME_MINUTES, MAX_PENDING_ACQUISITIONS);

        return resourceFactory;
    }

    /**
     * Creates an HttpClient with configured timeouts and connection settings.
     *
     * <p>This client ensures:
     * <ul>
     *   <li>Connection timeout prevents hanging on unresponsive servers</li>
     *   <li>Read/Write timeouts prevent indefinite hangs</li>
     *   <li>TCP_NODELAY enables immediate sending of small packets</li>
     *   <li>SO_KEEPALIVE maintains connection health for idle connections</li>
     * </ul>
     *
     * @param resourceFactory the configured resource factory
     * @return configured HttpClient
     */
    @Bean
    @ConditionalOnMissingBean
    public HttpClient investmentHttpClient(ReactorResourceFactory resourceFactory) {
        return HttpClient.create(resourceFactory.getConnectionProvider())
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_SECONDS * 1000)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true)
            .responseTimeout(java.time.Duration.ofSeconds(READ_TIMEOUT_SECONDS))
            .doOnConnected(connection -> connection
                .addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)));
    }

}

