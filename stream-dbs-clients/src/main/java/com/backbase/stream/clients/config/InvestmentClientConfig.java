package com.backbase.stream.clients.config;

import com.backbase.investment.api.service.ApiClient;
import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.ClientApi;
import com.backbase.investment.api.service.v1.FinancialAdviceApi;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.text.DateFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;

/**
 * Configuration for Investment service REST client (ClientApi).
 */
@Configuration
@ConfigurationProperties("backbase.communication.services.investment")
public class InvestmentClientConfig extends CompositeApiClientConfig {

    public static final String INVESTMENT_SERVICE_ID = "investment";

    public InvestmentClientConfig() {
        super(INVESTMENT_SERVICE_ID);
    }

    /**
     * Configuration for Investment service REST client (ClientApi).
     */
    @Bean
    @ConditionalOnMissingBean
    public ApiClient investmentApiClient(WebClient interServiceWebClient, ObjectMapper objectMapper, DateFormat dateFormat) {
        ObjectMapper mapper = objectMapper.copy();
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Builder mutate = interServiceWebClient.mutate();
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
    @ConditionalOnMissingBean
    public ClientApi clientApi(ApiClient investmentApiClient) {
        return new ClientApi(investmentApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public InvestmentProductsApi investmentProductsApi(ApiClient investmentApiClient) {
        return new InvestmentProductsApi(investmentApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public PortfolioApi portfolioApi(ApiClient investmentApiClient) {
        return new PortfolioApi(investmentApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public FinancialAdviceApi financialAdviceApi(ApiClient investmentApiClient) {
        return new FinancialAdviceApi(investmentApiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public AssetUniverseApi assetUniverseApi(ApiClient investmentApiClient) {
        return new AssetUniverseApi(investmentApiClient);
    }

}
