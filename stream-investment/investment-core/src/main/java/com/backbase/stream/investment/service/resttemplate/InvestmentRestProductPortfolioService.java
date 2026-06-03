package com.backbase.stream.investment.service.resttemplate;

import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_ADVICE_ENGINE;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_BADGE;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_DESCRIPTION;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_EXTERNAL_ID;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_EXTRA_DATA;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_IMAGE;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_MODEL_PORTFOLIO;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_NAME;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_ORDER;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_PRODUCT_CATEGORY;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_PRODUCT_TYPE;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_STATUS;

import com.backbase.investment.api.service.sync.ApiClient;
import com.backbase.investment.api.service.sync.ApiClient.CollectionFormat;
import com.backbase.investment.api.service.v1.model.InvestorModelPortfolio;
import com.backbase.investment.api.service.v1.model.PortfolioProduct;
import com.backbase.stream.configuration.IngestConfigProperties;
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
import reactor.core.publisher.Mono;

/**
 * RestTemplate-based service for portfolio product operations against the Investment service
 * {@code /service-api/v2/products/portfolio/} endpoints.
 *
 * <p>This service avoids the multipart serialisation issues present
 * in the auto-generated {@code InvestmentProductsApi} wrapper.
 *
 * <p>Mapping from the stream {@link ProductPortfolio} model to multipart form parameters is handled
 * internally by {@link #productPortfolioParams(ProductPortfolio)}.
 */
@Slf4j
@RequiredArgsConstructor
public class InvestmentRestProductPortfolioService {

    private final ApiClient apiClient;
    private final IngestConfigProperties ingestProperties;

    /**
     * Creates a new portfolio product via {@code POST /service-api/v2/products/portfolio/}.
     *
     * @param productPortfolio the stream portfolio product to create (must not be {@code null})
     * @param expand           optional fields to expand in the response
     * @return {@link Mono} emitting the created {@link PortfolioProduct}
     */
    public Mono<PortfolioProduct> createPortfolioProduct(ProductPortfolio productPortfolio,
        List<String> expand) {

        log.info("Starting portfolio product creation: name='{}', productType={}, externalId={}",
            productPortfolio.getName(), productPortfolio.getProductType(), productPortfolio.getExternalId());

        return Mono.defer(() -> Mono.just(invokeCreate(productPortfolio, expand)))
            .map(created -> {
                log.info("Portfolio product created successfully: uuid={}, name='{}', productType={}",
                    created.getUuid(), created.getName(), created.getProductType());
                return created;
            })
            .doOnError(throwable -> log.error(
                "Portfolio product creation failed: name='{}', productType={}, externalId={}, errorType={}, errorMessage={}",
                productPortfolio.getName(), productPortfolio.getProductType(), productPortfolio.getExternalId(),
                throwable.getClass().getSimpleName(), throwable.getMessage(), throwable));
    }

    /**
     * Updates an existing portfolio product via {@code PATCH /service-api/v2/products/portfolio/{uuid}/}.
     *
     * @param uuid          the UUID of the portfolio product to patch (must not be {@code null})
     * @param expand        optional fields to expand in the response
     * @param updateRequest the stream portfolio product with updated values
     * @return {@link Mono} emitting the patched {@link PortfolioProduct}
     */
    public Mono<PortfolioProduct> updatePortfolioProduct(String uuid, List<String> expand,
        ProductPortfolio updateRequest) {
        log.info("Starting portfolio product patch: uuid={}, name='{}', productType={}",
            uuid, updateRequest.getName(), updateRequest.getProductType());

        return Mono.defer(() -> {
                if (uuid == null) {
                    throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                        "Missing the required parameter 'uuid' when calling patchPortfolioProduct");
                }
                return Mono.just(invokeUpdate(uuid, expand, updateRequest));
            })
            .map(patched -> {
                log.info("Portfolio product patched successfully: uuid={}, name='{}', productType={}",
                    patched.getUuid(), patched.getName(), patched.getProductType());
                return patched;
            })
            .doOnError(throwable -> log.error(
                "Portfolio product patch failed: uuid={}, name='{}', productType={}, errorType={}, errorMessage={}",
                uuid, updateRequest.getName(), updateRequest.getProductType(),
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

    private PortfolioProduct invokeUpdate(String uuid, List<String> expand, ProductPortfolio data) {
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
        return apiClient.invokeAPI("/service-api/v2/products/portfolio/{uuid}/", HttpMethod.PUT, pathParams,
            queryParams, null, headerParams, cookieParams, formParams, localVarAccept, localVarContentType,
            new String[]{}, localVarReturnType).getBody();
    }

    private @NonNull MultiValueMap<String, Object> productPortfolioParams(ProductPortfolio data) {
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();
        Optional.ofNullable(data.getName())
            .ifPresent(v -> formParams.add(JSON_PROPERTY_NAME, v));
        Optional.ofNullable(data.getDescription())
            .ifPresent(v -> formParams.add(JSON_PROPERTY_DESCRIPTION, v));
        Optional.ofNullable(data.getBadge())
            .ifPresent(v -> formParams.add(JSON_PROPERTY_BADGE, v));
        Optional.ofNullable(data.getExternalId())
            .ifPresent(v -> formParams.add(JSON_PROPERTY_EXTERNAL_ID, v));
        Optional.ofNullable(data.getStatus())
            .ifPresent(v -> formParams.add(JSON_PROPERTY_STATUS, v));
        Optional.ofNullable(data.getOrder())
            .ifPresent(v -> formParams.add(JSON_PROPERTY_ORDER, v));
        Optional.ofNullable(data.getAdviceEngine())
            .ifPresent(v -> formParams.add(JSON_PROPERTY_ADVICE_ENGINE, v));
        Optional.ofNullable(data.getModelPortfolio())
            .ifPresent(v -> formParams.add(JSON_PROPERTY_MODEL_PORTFOLIO,
                Optional.of(v).map(InvestorModelPortfolio::getUuid).orElse(null)));
        Optional.ofNullable(data.getProductType())
            .ifPresent(v -> formParams.add(JSON_PROPERTY_PRODUCT_TYPE, v.getValue()));
        Optional.ofNullable(data.getProductCategory())
            .ifPresent(v -> formParams.add(JSON_PROPERTY_PRODUCT_CATEGORY, v));
        Optional.ofNullable(data.getExtraData())
            .ifPresent(v -> formParams.add(JSON_PROPERTY_EXTRA_DATA, v));
        if (ingestProperties.getPortfolio().isIngestImages()) {
            Optional.ofNullable(data.getImageResource()).ifPresent(v -> formParams.add(JSON_PROPERTY_IMAGE, v));
        }
        return formParams;
    }

}
