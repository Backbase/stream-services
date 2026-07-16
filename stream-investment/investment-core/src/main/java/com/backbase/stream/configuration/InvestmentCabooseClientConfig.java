package com.backbase.stream.configuration;

import com.backbase.investment.api.service.ApiClient;
import com.backbase.investment.api.service.v1.AllocationsApi;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.AsyncBulkGroupsApi;
import com.backbase.investment.api.service.v1.ClientApi;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * HTTP client configuration for investment-caboose API communication.
 *
 * <p>Mirrors {@link InvestmentClientConfig} exactly but targets investment-caboose via
 * {@code backbase.communication.services.investment-caboose.directUri}. Registers a dedicated
 * {@code cabooseInvestmentApiClient} and all individual investment API beans with a
 * {@code caboose} prefix so they can coexist alongside primary beans in dual-write mode.
 *
 * <p>This configuration is only loaded when
 * {@code backbase.bootstrap.ingestions.investment.caboose.enabled=true}.
 */
@Configuration
@ConditionalOnProperty(name = "backbase.bootstrap.ingestions.investment.caboose.enabled")
@ConfigurationProperties("backbase.communication.services.investment-caboose")
public class InvestmentCabooseClientConfig extends CompositeApiClientConfig {

    public static final String INVESTMENT_CABOOSE_SERVICE_ID = "investment-caboose";

    public InvestmentCabooseClientConfig() {
        super(INVESTMENT_CABOOSE_SERVICE_ID);
    }

    /**
     * Reactive {@link ApiClient} pointed at investment-caboose.
     *
     * <p>Uses a dedicated {@code cabooseHttpClient} so the caboose connection pool is
     * completely independent of the primary Investment Service pool.
     */
    @Bean("cabooseInvestmentApiClient")
    public ApiClient cabooseInvestmentApiClient(
        WebClient interServiceWebClient,
        @Qualifier("cabooseHttpClient") HttpClient cabooseHttpClient,
        ObjectMapper objectMapper,
        DateFormat dateFormat) {
        ObjectMapper mapper = objectMapper.copy();
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Builder mutate = interServiceWebClient.mutate()
            .clientConnector(new ReactorClientHttpConnector(cabooseHttpClient));
        mutate.codecs(clientCodecConfigurer -> {
            Jackson2JsonEncoder encoder = new Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON);
            Jackson2JsonDecoder decoder = new Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON);
            clientCodecConfigurer.defaultCodecs().jackson2JsonEncoder(encoder);
            clientCodecConfigurer.defaultCodecs().jackson2JsonDecoder(decoder);
        });
        return new ApiClient(mutate.build(), mapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean("cabooseClientApi")
    public ClientApi cabooseClientApi(
        @Qualifier("cabooseInvestmentApiClient") ApiClient cabooseInvestmentApiClient) {
        return new ClientApi(cabooseInvestmentApiClient);
    }

    @Bean("cabooseInvestmentProductsApi")
    public InvestmentProductsApi cabooseInvestmentProductsApi(
        @Qualifier("cabooseInvestmentApiClient") ApiClient cabooseInvestmentApiClient) {
        return new InvestmentProductsApi(cabooseInvestmentApiClient);
    }

    @Bean("caboosePortfolioApi")
    public PortfolioApi caboosePortfolioApi(
        @Qualifier("cabooseInvestmentApiClient") ApiClient cabooseInvestmentApiClient) {
        return new PortfolioApi(cabooseInvestmentApiClient);
    }

    @Bean("cabooseFinancialAdviceApi")
    public FinancialAdviceApi cabooseFinancialAdviceApi(
        @Qualifier("cabooseInvestmentApiClient") ApiClient cabooseInvestmentApiClient) {
        return new FinancialAdviceApi(cabooseInvestmentApiClient);
    }

    @Bean("cabooseAssetUniverseApi")
    public AssetUniverseApi cabooseAssetUniverseApi(
        @Qualifier("cabooseInvestmentApiClient") ApiClient cabooseInvestmentApiClient) {
        return new AssetUniverseApi(cabooseInvestmentApiClient);
    }

    @Bean("cabooseAllocationsApi")
    public AllocationsApi cabooseAllocationsApi(
        @Qualifier("cabooseInvestmentApiClient") ApiClient cabooseInvestmentApiClient) {
        return new AllocationsApi(cabooseInvestmentApiClient);
    }

    @Bean("cabooseInvestmentApi")
    public InvestmentApi cabooseInvestmentApi(
        @Qualifier("cabooseInvestmentApiClient") ApiClient cabooseInvestmentApiClient) {
        return new InvestmentApi(cabooseInvestmentApiClient);
    }

    @Bean("caboosePaymentsApi")
    public PaymentsApi caboosePaymentsApi(
        @Qualifier("cabooseInvestmentApiClient") ApiClient cabooseInvestmentApiClient) {
        return new PaymentsApi(cabooseInvestmentApiClient);
    }

    @Bean("caboosePortfolioTradingAccountsApi")
    public PortfolioTradingAccountsApi caboosePortfolioTradingAccountsApi(
        @Qualifier("cabooseInvestmentApiClient") ApiClient cabooseInvestmentApiClient) {
        return new PortfolioTradingAccountsApi(cabooseInvestmentApiClient);
    }

    @Bean("cabooseCurrencyApi")
    public CurrencyApi cabooseCurrencyApi(
        @Qualifier("cabooseInvestmentApiClient") ApiClient cabooseInvestmentApiClient) {
        return new CurrencyApi(cabooseInvestmentApiClient);
    }

    @Bean("cabooseRiskAssessmentApi")
    public RiskAssessmentApi cabooseRiskAssessmentApi(
        @Qualifier("cabooseInvestmentApiClient") ApiClient cabooseInvestmentApiClient) {
        return new RiskAssessmentApi(cabooseInvestmentApiClient);
    }

    @Bean("cabooseAsyncBulkGroupsApi")
    public AsyncBulkGroupsApi cabooseAsyncBulkGroupsApi(
        @Qualifier("cabooseInvestmentApiClient") ApiClient cabooseInvestmentApiClient) {
        return new AsyncBulkGroupsApi(cabooseInvestmentApiClient);
    }
}
