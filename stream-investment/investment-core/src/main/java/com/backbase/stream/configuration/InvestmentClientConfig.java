package com.backbase.stream.configuration;

import com.backbase.investment.api.service.ApiClient;
import com.backbase.investment.api.service.v1.AllocationsApi;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.AsyncBulkGroupsApi;
import com.backbase.investment.api.service.v1.ClientApi;
import com.backbase.investment.api.service.v1.ContentApi;
import com.backbase.investment.api.service.v1.CurrencyApi;
import com.backbase.investment.api.service.v1.FinancialAdviceApi;
import com.backbase.investment.api.service.v1.InvestmentApi;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PaymentsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.backbase.investment.api.service.v1.PortfolioTradingAccountsApi;
import com.backbase.investment.api.service.v1.RiskAssessmentApi;
import com.backbase.stream.clients.config.CompositeApiClientConfig;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.text.DateFormat;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import reactor.netty.http.client.HttpClient;

/**
 * Configuration for Investment service REST client (ClientApi).
 *
 * <p>This configuration creates the Investment API client with proper codec configuration.
 *
 * <p><strong>Note:</strong> Connection pooling, timeouts, and rate limiting are configured in
 * {@link InvestmentWebClientConfiguration} which should be imported alongside this class.
 * The WebClient connection pool prevents resource exhaustion and 503 errors by limiting:
 * <ul>
 *   <li>Maximum concurrent connections to 100 (configurable)</li>
 *   <li>Connection acquisition timeout to 45 seconds</li>
 *   <li>Read/Write timeouts to 30 seconds each</li>
 * </ul>
 */
@Configuration
@ConditionalOnBean(InvestmentServiceConfiguration.class)
@ConfigurationProperties("backbase.communication.services.investment")
public class InvestmentClientConfig extends CompositeApiClientConfig {

    public static final String INVESTMENT_SERVICE_ID = "investment";

    public InvestmentClientConfig() {
        super(INVESTMENT_SERVICE_ID);
    }

    /**
     * Configuration for Investment service REST client (ClientApi).
     */
    @Bean("investmentApiClient")
    public ApiClient investmentApiClient(WebClient interServiceWebClient,
        @Qualifier("investmentHttpClient") HttpClient investmentHttpClient,
        ObjectMapper objectMapper,
        DateFormat dateFormat) {
        ObjectMapper mapper = objectMapper.copy();
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Builder mutate = interServiceWebClient.mutate()
            .clientConnector(new ReactorClientHttpConnector(investmentHttpClient));
        mutate.codecs(clientCodecConfigurer -> {
            Jackson2JsonEncoder encoder = new Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON);
            Jackson2JsonDecoder decoder = new Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON);
            clientCodecConfigurer.defaultCodecs().jackson2JsonEncoder(encoder);
            clientCodecConfigurer.defaultCodecs().jackson2JsonDecoder(decoder);
        });
        return new ApiClient(mutate.build(), mapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    public ClientApi clientApi(@Qualifier("investmentApiClient") ApiClient investmentApiClient) {
        return new ClientApi(investmentApiClient);
    }

    @Bean
    public InvestmentProductsApi investmentProductsApi(
        @Qualifier("investmentApiClient") ApiClient investmentApiClient) {
        return new InvestmentProductsApi(investmentApiClient);
    }

    @Bean
    public PortfolioApi portfolioApi(@Qualifier("investmentApiClient") ApiClient investmentApiClient) {
        return new PortfolioApi(investmentApiClient);
    }

    @Bean
    public FinancialAdviceApi financialAdviceApi(@Qualifier("investmentApiClient") ApiClient investmentApiClient) {
        return new FinancialAdviceApi(investmentApiClient);
    }

    @Bean
    public AssetUniverseApi assetUniverseApi(@Qualifier("investmentApiClient") ApiClient investmentApiClient) {
        return new AssetUniverseApi(investmentApiClient);
    }

    @Bean
    public AllocationsApi allocationsApi(@Qualifier("investmentApiClient") ApiClient investmentApiClient) {
        return new AllocationsApi(investmentApiClient);
    }

    @Bean
    public InvestmentApi investmentApi(@Qualifier("investmentApiClient") ApiClient investmentApiClient) {
        return new InvestmentApi(investmentApiClient);
    }

    @Bean
    public ContentApi contentApi(@Qualifier("investmentApiClient") ApiClient investmentApiClient) {
        return new ContentApi(investmentApiClient);
    }

    @Bean
    public PaymentsApi paymentsApi(@Qualifier("investmentApiClient") ApiClient investmentApiClient) {
        return new PaymentsApi(investmentApiClient);
    }

    @Bean
    public PortfolioTradingAccountsApi portfolioTradingAccountsApi(
        @Qualifier("investmentApiClient") ApiClient investmentApiClient) {
        return new PortfolioTradingAccountsApi(investmentApiClient);
    }

    @Bean
    public CurrencyApi currencyApi(@Qualifier("investmentApiClient") ApiClient investmentApiClient) {
        return new CurrencyApi(investmentApiClient);
    }

    @Bean
    public RiskAssessmentApi riskAssessmentApi(@Qualifier("investmentApiClient") ApiClient investmentApiClient) {
        return new RiskAssessmentApi(investmentApiClient);
    }

    @Bean
    public AsyncBulkGroupsApi asyncBulkGroupsApi(@Qualifier("investmentApiClient") ApiClient investmentApiClient) {
        return new AsyncBulkGroupsApi(investmentApiClient);
    }

}
