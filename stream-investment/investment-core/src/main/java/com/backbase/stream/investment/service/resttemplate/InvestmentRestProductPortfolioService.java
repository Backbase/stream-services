package com.backbase.stream.investment.service.resttemplate;

import com.backbase.investment.api.service.sync.ApiClient;
import com.backbase.investment.api.service.sync.ApiClient.CollectionFormat;
import com.backbase.investment.api.service.v1.model.PortfolioProduct;
import com.backbase.stream.configuration.IngestConfigProperties;
import com.backbase.stream.investment.ModelPortfolio;
import com.backbase.stream.investment.ProductPortfolio;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * RestTemplate-based service for model portfolio operations against the Investment service {@code /service-api/v2}
 * endpoints.
 *
 * <p>This service avoids the multipart serialisation issues present
 * in the auto-generated {@code FinancialAdviceApi} wrapper.
 *
 * <p>Mapping from the stream {@link ModelPortfolio} model to the OAS request DTO is handled
 * internally by {@link RestTemplateModelPortfolioMapper}.
 */
@Slf4j
@RequiredArgsConstructor
public class InvestmentRestProductPortfolioService {

    private final ApiClient apiClient;
    private final IngestConfigProperties ingestProperties;

    public Mono<PortfolioProduct> createPortfolioProduct(ProductPortfolio productPortfolio,
        List<String> expand) {

        log.info("Starting model portfolio creation: name='{}', riskLevel={}",
            productPortfolio.getName(), productPortfolio.getOrder());

        return Mono.defer(() -> Mono.just(invokeCreate(productPortfolio, expand)))
            .map(created -> {
                log.info("Model portfolio created successfully: uuid={}, name='{}'",
                    created.getUuid(), created.getName());
                return created;
            })
            .doOnError(throwable -> log.error(
                "Model portfolio creation failed: name='{}', riskLevel={}, errorType={}, errorMessage={}",
                productPortfolio.getName(), productPortfolio.getOrder(),
                throwable.getClass().getSimpleName(), throwable.getMessage(), throwable));
    }

    public Mono<PortfolioProduct> patchPortfolioProduct(String uuid, List<String> expand,
        ProductPortfolio updateRequest)
        throws WebClientResponseException {
        log.info("Starting model portfolio patch: uuid={}, name='{}'", uuid, updateRequest.getName());

        return Mono.defer(() -> {
                if (uuid == null) {
                    throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                        "Missing the required parameter 'uuid' when calling patchModelPortfolio");
                }
                return Mono.just(invokePatch(uuid, expand, updateRequest));
            })
            .map(patched -> {
                log.info("Model portfolio patched successfully: uuid={}, name='{}'",
                    patched.getUuid(), patched.getName());
                return patched;
            })
            .doOnError(throwable -> log.error(
                "Model portfolio patch failed: uuid={}, name='{}', errorType={}, errorMessage={}",
                uuid, updateRequest.getName(),
                throwable.getClass().getSimpleName(), throwable.getMessage(), throwable));
    }

    private PortfolioProduct invokeCreate(ProductPortfolio portfolioProduct, List<String> expand) {
        final MultiValueMap<String, Object> formParams = productPortfolioParams(portfolioProduct);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(
            CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "expand", expand));

        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(new String[]{"application/json"});
        final MediaType localVarContentType = apiClient.selectHeaderContentType(new String[]{"multipart/form-data"});

        ParameterizedTypeReference<PortfolioProduct> localVarReturnType = new ParameterizedTypeReference<>() {
        };

        return apiClient.invokeAPI("/service-api/v2/products/portfolio/", HttpMethod.POST,
                Collections.emptyMap(), queryParams, null, headerParams,
                cookieParams, formParams, localVarAccept, localVarContentType, new String[]{}, localVarReturnType)
            .getBody();
    }

    private PortfolioProduct invokePatch(String uuid, List<String> expand, ProductPortfolio data) {
        final Map<String, Object> pathParams = new HashMap<String, Object>();
        pathParams.put("uuid", uuid);
        final MultiValueMap<String, Object> formParams = productPortfolioParams(data);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(
            CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "expand", expand));

        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(new String[]{"application/json"});
        final MediaType localVarContentType = apiClient.selectHeaderContentType(new String[]{"multipart/form-data"});

        ParameterizedTypeReference<PortfolioProduct> localVarReturnType = new ParameterizedTypeReference<>() {
        };
        return apiClient.invokeAPI("/service-api/v2/products/portfolio/{uuid}/", HttpMethod.PATCH, pathParams,
            queryParams, null, headerParams, cookieParams, formParams, localVarAccept, localVarContentType,
            new String[]{}, localVarReturnType).getBody();
    }

    private @NonNull MultiValueMap<String, Object> productPortfolioParams(ProductPortfolio data) {
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();
        Optional.ofNullable(data.getName())
            .ifPresent(v -> formParams.add("name", v));
        Optional.ofNullable(data.getDescription())
            .ifPresent(v -> formParams.add("description", v));
        Optional.ofNullable(data.getBadge())
            .ifPresent(v -> formParams.add("badge", v));
        Optional.ofNullable(data.getExternalId())
            .ifPresent(v -> formParams.add("external_id", v));
        Optional.ofNullable(data.getStatus())
            .ifPresent(v -> formParams.add("status", v));
        Optional.ofNullable(data.getOrder())
            .ifPresent(v -> formParams.add("order", v));
        Optional.ofNullable(data.getAdviceEngine())
            .ifPresent(v -> formParams.add("advice_engine", v));
        Optional.ofNullable(data.getModelPortfolio())
            .ifPresent(v -> formParams.add("model_portfolio", v));
        Optional.ofNullable(data.getProductType())
            .ifPresent(v -> formParams.add("product_type", v));
        Optional.ofNullable(data.getProductCategory())
            .ifPresent(v -> formParams.add("product_category", v));
        Optional.ofNullable(data.getExtraData())
            .ifPresent(v -> formParams.add("extra_data", v));
        if (ingestProperties.getPortfolio().isIngestImages()) {
            Optional.ofNullable(data.getImageResource()).ifPresent(v -> formParams.add("image", v));
        }
        return formParams;
    }

}
