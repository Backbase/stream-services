package com.backbase.stream.configuration;

import com.backbase.stream.investment.service.resttemplate.InvestmentRestAssetUniverseService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestModelPortfolioService;
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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * Sync REST-template client configuration for investment-caboose.
 *
 * <p>Mirrors {@link InvestmentRestServiceApiConfiguration} but targets investment-caboose.
 * The service ID is configurable via
 * {@code backbase.investment.communication.integration.caboose.serviceId} and defaults to
 * {@value #DEFAULT_SERVICE_ID}.
 *
 * <p>This configuration is only loaded when {@link InvestmentCabooseServiceConfiguration} is
 * active (i.e. {@code backbase.bootstrap.ingestions.investment.caboose.enabled=true}).
 *
 * <p>All beans carry an explicit {@code cabooseRest} / {@code cabooseInvestmentRest} name prefix
 * so they coexist with the primary {@link InvestmentRestServiceApiConfiguration} beans.
 */
@Setter
@Configuration
@ConditionalOnBean(InvestmentCabooseServiceConfiguration.class)
@ConfigurationProperties(prefix = "backbase.investment.communication.integration.caboose")
public class InvestmentCabooseRestServiceApiConfiguration {

    static final String DEFAULT_SERVICE_ID = "investment-caboose";

    private String serviceId = DEFAULT_SERVICE_ID;
    private String serviceUrl = "";

    @Value("${backbase.communication.http.default-scheme:http}")
    @Pattern(regexp = "https?")
    private String scheme;

    /**
     * Sync {@link com.backbase.investment.api.service.sync.ApiClient} pointed at investment-caboose.
     *
     * <p>Mirrors {@link InvestmentRestServiceApiConfiguration#restInvestmentApiClient} exactly:
     * uses the shared {@code interServiceRestTemplate} directly (no copy) so that all auth
     * configuration — interceptors, request factory, credentials — is naturally preserved.
     */
    @Bean("cabooseRestInvestmentApiClient")
    public com.backbase.investment.api.service.sync.ApiClient cabooseRestInvestmentApiClient(
        @Qualifier("interServiceRestTemplate") RestTemplate restTemplate,
        @Qualifier("cabooseRestInvestmentObjectMapper") ObjectMapper cabooseRestInvestmentObjectMapper) {

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(cabooseRestInvestmentObjectMapper);

        restTemplate.getMessageConverters().removeIf(m -> m instanceof MappingJackson2HttpMessageConverter);
        restTemplate.getMessageConverters().add(converter);

        com.backbase.investment.api.service.sync.ApiClient apiClient =
            new com.backbase.investment.api.service.sync.ApiClient(restTemplate);
        apiClient.setBasePath(scheme + "://" + serviceId + serviceUrl);
        return apiClient;
    }

    @Bean("cabooseRestInvestmentObjectMapper")
    public ObjectMapper cabooseRestInvestmentObjectMapper(ObjectMapper legacyObjectMapper) {
        ObjectMapper mapper = legacyObjectMapper.copy();
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean("cabooseRestAssetUniverseApi")
    public com.backbase.investment.api.service.sync.v1.AssetUniverseApi cabooseRestAssetUniverseApi(
        @Qualifier("cabooseRestInvestmentApiClient")
        com.backbase.investment.api.service.sync.ApiClient cabooseRestInvestmentApiClient) {
        return new com.backbase.investment.api.service.sync.v1.AssetUniverseApi(cabooseRestInvestmentApiClient);
    }

    @Bean("cabooseInvestmentRestModelPortfolioService")
    public InvestmentRestModelPortfolioService cabooseInvestmentRestModelPortfolioService(
        @Qualifier("cabooseRestInvestmentApiClient")
        com.backbase.investment.api.service.sync.ApiClient cabooseRestInvestmentApiClient) {
        return new InvestmentRestModelPortfolioService(cabooseRestInvestmentApiClient);
    }

    @Bean("cabooseInvestmentRestProductPortfolioService")
    public InvestmentRestProductPortfolioService cabooseInvestmentRestProductPortfolioService(
        @Qualifier("cabooseRestInvestmentApiClient")
        com.backbase.investment.api.service.sync.ApiClient cabooseRestInvestmentApiClient,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentRestProductPortfolioService(cabooseRestInvestmentApiClient, portfolioProperties);
    }

    @Bean("cabooseInvestmentRestAssetUniverseService")
    public InvestmentRestAssetUniverseService cabooseInvestmentRestAssetUniverseService(
        @Qualifier("cabooseRestInvestmentApiClient")
        com.backbase.investment.api.service.sync.ApiClient cabooseRestInvestmentApiClient,
        IngestConfigProperties portfolioProperties) {
        return new InvestmentRestAssetUniverseService(cabooseRestInvestmentApiClient, portfolioProperties);
    }
}

