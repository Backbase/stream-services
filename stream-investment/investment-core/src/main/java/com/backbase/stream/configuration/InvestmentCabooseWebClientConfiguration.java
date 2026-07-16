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
 * WebClient configuration for investment-caboose service communication.
 *
 * <p>Mirrors {@link InvestmentWebClientConfiguration} but produces a dedicated, separately named
 * connection pool and HTTP client for investment-caboose so that both targets (Investment Service
 * and investment-caboose) have independent resource budgets and do not interfere with each other.
 *
 * <p>All tunable values are externalized via {@link InvestmentCabooseWebClientProperties} and can
 * be overridden through {@code application.yml} without recompiling.
 *
 * <p>All beans are explicitly named ({@code cabooseConnectionProvider} and
 * {@code cabooseHttpClient}) so they never clash with the primary investment beans.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(InvestmentCabooseWebClientProperties.class)
public class InvestmentCabooseWebClientConfiguration {

    /**
     * Dedicated {@link ConnectionProvider} for the investment-caboose client pool.
     *
     * @param props investment-caboose HTTP-client configuration properties
     * @return caboose-specific ConnectionProvider
     */
    @Bean("cabooseConnectionProvider")
    public ConnectionProvider cabooseConnectionProvider(InvestmentCabooseWebClientProperties props) {
        ConnectionProvider provider = ConnectionProvider.builder("investment-caboose-client-pool")
            .maxConnections(props.getMaxConnections())
            .maxIdleTime(Duration.ofMinutes(props.getMaxIdleTimeMinutes()))
            .maxLifeTime(Duration.ofMinutes(props.getMaxLifeTimeMinutes()))
            .pendingAcquireMaxCount(props.getMaxPendingAcquires())
            .pendingAcquireTimeout(Duration.ofMillis(props.getPendingAcquireTimeoutMillis()))
            .evictInBackground(Duration.ofSeconds(props.getEvictInBackgroundSeconds()))
            .build();

        log.info("Configured investment-caboose ConnectionProvider: maxConnections={}, maxIdleTime={}min,"
                + " pendingAcquireMaxCount={}, pendingAcquireTimeout={}ms",
            props.getMaxConnections(), props.getMaxIdleTimeMinutes(), props.getMaxPendingAcquires(),
            props.getPendingAcquireTimeoutMillis());

        return provider;
    }

    /**
     * Creates an {@link HttpClient} backed by the caboose-specific connection pool and with
     * explicit connect / read / write timeouts.
     *
     * @param connectionProvider the caboose-specific connection provider
     * @param props              investment-caboose HTTP-client configuration properties
     * @return configured HttpClient targeting investment-caboose
     */
    @Bean("cabooseHttpClient")
    public HttpClient cabooseHttpClient(
        @Qualifier("cabooseConnectionProvider") ConnectionProvider connectionProvider,
        InvestmentCabooseWebClientProperties props) {
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

