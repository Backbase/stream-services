package com.backbase.stream.configuration;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
 * <p>All tunable values are externalized via {@link InvestmentWebClientProperties} and can be
 * overridden through {@code application.yml} without recompiling.
 *
 * <p>All beans are explicitly named so they are unambiguous and never accidentally
 * replaced by Spring Boot auto-configuration or another generic bean of the same type.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(InvestmentWebClientProperties.class)
public class InvestmentWebClientConfiguration {

    /**
     * Dedicated {@link ConnectionProvider} for the Investment service client pool.
     *
     * <p>Using an explicit named bean (rather than {@code @ConditionalOnMissingBean ReactorResourceFactory})
     * avoids silently falling back to Spring Boot's auto-configured shared pool when one already exists
     * in the application context.
     *
     * @param props investment HTTP-client configuration properties
     * @return investment-specific ConnectionProvider
     */
    @Bean("investmentConnectionProvider")
    public ConnectionProvider investmentConnectionProvider(InvestmentWebClientProperties props) {
        ConnectionProvider provider = ConnectionProvider.builder("investment-client-pool")
            .maxConnections(props.getMaxConnections())
            .maxIdleTime(Duration.ofMinutes(props.getMaxIdleTimeMinutes()))
            .maxLifeTime(Duration.ofMinutes(props.getMaxLifeTimeMinutes()))
            .pendingAcquireMaxCount(props.getMaxPendingAcquires())
            .pendingAcquireTimeout(Duration.ofMillis(props.getPendingAcquireTimeoutMillis()))
            .evictInBackground(Duration.ofSeconds(props.getEvictInBackgroundSeconds()))
            .build();

        log.info("Configured investment ConnectionProvider: maxConnections={}, maxIdleTime={}min,"
                + " pendingAcquireMaxCount={}",
            props.getMaxConnections(), props.getMaxIdleTimeMinutes(), props.getMaxPendingAcquires());

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
     * @param props              investment HTTP-client configuration properties
     * @return configured HttpClient
     */
    @Bean("investmentHttpClient")
    public HttpClient investmentHttpClient(
        @Qualifier("investmentConnectionProvider") ConnectionProvider connectionProvider,
        InvestmentWebClientProperties props) {
        return HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getConnectTimeoutSeconds() * 1000)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true)
            .responseTimeout(Duration.ofSeconds(props.getReadTimeoutSeconds()))
            .doOnConnected(connection -> connection
                .addHandlerLast(new ReadTimeoutHandler(props.getReadTimeoutSeconds(), TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(props.getWriteTimeoutSeconds(), TimeUnit.SECONDS)));
    }

}
