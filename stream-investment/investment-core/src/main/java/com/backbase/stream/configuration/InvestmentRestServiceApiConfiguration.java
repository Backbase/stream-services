package com.backbase.stream.configuration;

import com.backbase.investment.api.service.sync.v1.ContentApi;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestAssetUniverseService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestDocumentContentService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestModelPortfolioService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestNewsContentService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestProductPortfolioService;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.validation.constraints.Pattern;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Setter
@Configuration
@ConditionalOnBean(InvestmentServiceConfiguration.class)
@ConfigurationProperties(prefix = "backbase.investment.communication.integration")
public class InvestmentRestServiceApiConfiguration {

    private String serviceId = "investment";
    private String serviceUrl = "";

    @Value("${backbase.communication.http.default-scheme:http}")
    @Pattern(regexp = "https?")
    private String scheme;

    /**
     * Configuration for Investment service REST client (ClientApi).
     */
    @Bean("restInvestmentApiClient")
    public com.backbase.investment.api.service.sync.ApiClient restInvestmentApiClient(
        @Qualifier("interServiceRestTemplate") RestTemplate restTemplate,
        @Qualifier("restInvestmentObjectMapper") ObjectMapper restInvestmentObjectMapper) {

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(restInvestmentObjectMapper);

        restTemplate.getMessageConverters().removeIf(m -> m instanceof MappingJackson2HttpMessageConverter);
        restTemplate.getMessageConverters().add(converter);

        com.backbase.investment.api.service.sync.ApiClient apiClient = new com.backbase.investment.api.service.sync.ApiClient(
            restTemplate);
        apiClient.setBasePath(scheme + "://" + serviceId + serviceUrl);
        return apiClient;
    }

    @Bean("restInvestmentObjectMapper")
    public ObjectMapper restInvestmentObjectMapper(ObjectMapper legacyObjectMapper) {
        ObjectMapper mapper = legacyObjectMapper.copy();
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean("restContentApi")
    public com.backbase.investment.api.service.sync.v1.ContentApi restContentApi(
        @Qualifier("restInvestmentApiClient")
        com.backbase.investment.api.service.sync.ApiClient restInvestmentApiClient) {
        return new com.backbase.investment.api.service.sync.v1.ContentApi(restInvestmentApiClient);
    }

    @Bean("restAssetUniverseApi")
    public com.backbase.investment.api.service.sync.v1.AssetUniverseApi restAssetUniverseApi(
        @Qualifier("restInvestmentApiClient")
        com.backbase.investment.api.service.sync.ApiClient restInvestmentApiClient) {
        return new com.backbase.investment.api.service.sync.v1.AssetUniverseApi(restInvestmentApiClient);
    }

    @Bean
    @Primary
    public InvestmentRestNewsContentService investmentNewsContentService(
        @Qualifier("restContentApi") ContentApi restContentApi,
        @Qualifier("restInvestmentApiClient")
        com.backbase.investment.api.service.sync.ApiClient restInvestmentApiClient) {
        return new InvestmentRestNewsContentService(restContentApi, restInvestmentApiClient);
    }

    @Bean
    @Primary
    public InvestmentRestDocumentContentService investmentRestContentDocumentService(
        @Qualifier("restContentApi") ContentApi restContentApi,
        @Qualifier("restInvestmentApiClient")
        com.backbase.investment.api.service.sync.ApiClient restInvestmentApiClient) {
        return new InvestmentRestDocumentContentService(restContentApi, restInvestmentApiClient);
    }

    @Bean
    @Primary
    public InvestmentRestAssetUniverseService investmentRestAssetUniverseService(
        @Qualifier("restInvestmentApiClient")
        com.backbase.investment.api.service.sync.ApiClient restInvestmentApiClient,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentRestAssetUniverseService(restInvestmentApiClient, portfolioProperties);
    }

    @Bean
    @Primary
    public InvestmentRestModelPortfolioService investmentRestModelPortfolioService(
        @Qualifier("restInvestmentApiClient")
        com.backbase.investment.api.service.sync.ApiClient restInvestmentApiClient) {
        return new InvestmentRestModelPortfolioService(restInvestmentApiClient);
    }

    @Bean
    @Primary
    public InvestmentRestProductPortfolioService investmentRestProductPortfolioService(
        @Qualifier("restInvestmentApiClient")
        com.backbase.investment.api.service.sync.ApiClient restInvestmentApiClient,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentRestProductPortfolioService(restInvestmentApiClient, portfolioProperties);
    }


}
