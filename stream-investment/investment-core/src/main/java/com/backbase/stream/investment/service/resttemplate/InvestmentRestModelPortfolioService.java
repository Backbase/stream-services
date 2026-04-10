package com.backbase.stream.investment.service.resttemplate;

import com.backbase.investment.api.service.sync.ApiClient;
import com.backbase.investment.api.service.v1.model.OASModelPortfolioRequestDataRequest;
import com.backbase.investment.api.service.v1.model.OASModelPortfolioResponse;
import com.backbase.stream.investment.ModelPortfolio;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Mono;

/**
 * RestTemplate-based service for model portfolio operations against the Investment
 * service {@code /service-api/v2} endpoints.
 *
 * <p>This service avoids the multipart serialisation issues present
 * in the auto-generated {@code FinancialAdviceApi} wrapper.
 *
 * <p>Mapping from the stream {@link ModelPortfolio} model to the OAS request DTO is handled
 * internally by {@link RestTemplateModelPortfolioMapper}.
 */
@Slf4j
@RequiredArgsConstructor
public class InvestmentRestModelPortfolioService {

    private static final String MODEL_PORTFOLIOS_PATH =
        "/service-api/v2/advice-engines/model-portfolio/model_portfolios/";
    private static final String MODEL_PORTFOLIO_BY_UUID_PATH =
        "/service-api/v2/advice-engines/model-portfolio/model_portfolios/{uuid}/";

    private final ApiClient apiClient;
    private final RestTemplateModelPortfolioMapper modelPortfolioMapper =
        Mappers.getMapper(RestTemplateModelPortfolioMapper.class);

    /**
     * Creates a new model portfolio via {@code POST /service-api/v2/.../model_portfolios/}.
     *
     * @param modelPortfolio the stream model portfolio to create (must not be {@code null})
     * @return {@link Mono} emitting the created {@link OASModelPortfolioResponse}
     */
    public Mono<OASModelPortfolioResponse> createModelPortfolio(ModelPortfolio modelPortfolio) {
        OASModelPortfolioRequestDataRequest request = modelPortfolioMapper.toRequest(modelPortfolio);

        log.info("Starting model portfolio creation: name='{}', riskLevel={}",
            modelPortfolio.getName(), modelPortfolio.getRiskLevel());

        return Mono.defer(() -> Mono.just(invokeCreate(request)))
            .map(created -> {
                log.info("Model portfolio created successfully: uuid={}, name='{}'",
                    created.getUuid(), created.getName());
                return created;
            })
            .doOnError(throwable -> log.error(
                "Model portfolio creation failed: name='{}', riskLevel={}, errorType={}, errorMessage={}",
                modelPortfolio.getName(), modelPortfolio.getRiskLevel(),
                throwable.getClass().getSimpleName(), throwable.getMessage(), throwable));
    }

    /**
     * Patches an existing model portfolio via {@code PATCH /service-api/v2/.../model_portfolios/{uuid}/}.
     *
     * @param uuid           the UUID of the model portfolio to patch (must not be {@code null})
     * @param modelPortfolio the stream model portfolio with updated values
     * @return {@link Mono} emitting the patched {@link OASModelPortfolioResponse}
     */
    public Mono<OASModelPortfolioResponse> patchModelPortfolio(String uuid, ModelPortfolio modelPortfolio) {
        OASModelPortfolioRequestDataRequest request = modelPortfolioMapper.toRequest(modelPortfolio);

        log.info("Starting model portfolio patch: uuid={}, name='{}'", uuid, modelPortfolio.getName());

        return Mono.defer(() -> {
            if (uuid == null) {
                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                    "Missing the required parameter 'uuid' when calling patchModelPortfolio");
            }
            return Mono.just(invokePatch(uuid, request));
        })
        .map(patched -> {
            log.info("Model portfolio patched successfully: uuid={}, name='{}'",
                patched.getUuid(), patched.getName());
            return patched;
        })
        .doOnError(throwable -> log.error(
            "Model portfolio patch failed: uuid={}, name='{}', errorType={}, errorMessage={}",
            uuid, modelPortfolio.getName(),
            throwable.getClass().getSimpleName(), throwable.getMessage(), throwable));
    }

    private OASModelPortfolioResponse invokeCreate(OASModelPortfolioRequestDataRequest data) {
        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();

        if (data != null) {
            formParams.add("data", data);
        }

        final List<MediaType> accept = apiClient.selectHeaderAccept(new String[]{"application/json"});
        final MediaType contentType = apiClient.selectHeaderContentType(new String[]{"multipart/form-data"});

        ParameterizedTypeReference<OASModelPortfolioResponse> returnType =
            new ParameterizedTypeReference<OASModelPortfolioResponse>() {};

        return apiClient.invokeAPI(MODEL_PORTFOLIOS_PATH, HttpMethod.POST,
                Collections.emptyMap(), queryParams, null, headerParams,
                cookieParams, formParams, accept, contentType, new String[]{}, returnType)
            .getBody();
    }

    private OASModelPortfolioResponse invokePatch(String uuid, OASModelPortfolioRequestDataRequest data) {
        final Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("uuid", uuid);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();

        if (data != null) {
            formParams.add("data", data);
        }

        final List<MediaType> accept = apiClient.selectHeaderAccept(new String[]{"application/json"});
        final MediaType contentType = apiClient.selectHeaderContentType(new String[]{"multipart/form-data"});

        ParameterizedTypeReference<OASModelPortfolioResponse> returnType =
            new ParameterizedTypeReference<OASModelPortfolioResponse>() {};

        return apiClient.invokeAPI(MODEL_PORTFOLIO_BY_UUID_PATH, HttpMethod.PATCH,
                uriVariables, queryParams, null, headerParams,
                cookieParams, formParams, accept, contentType, new String[]{}, returnType)
            .getBody();
    }
}
