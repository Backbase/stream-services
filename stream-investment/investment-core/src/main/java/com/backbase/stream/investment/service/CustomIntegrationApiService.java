package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.ApiClient;
import com.backbase.investment.api.service.v1.model.Asset;
import com.backbase.investment.api.service.v1.model.OASAssetRequestDataRequest;
import com.backbase.investment.api.service.v1.model.OASModelPortfolioRequestDataRequest;
import com.backbase.investment.api.service.v1.model.OASModelPortfolioResponse;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Deprecated(forRemoval = true, since = "8.6.0")
@Slf4j
@RequiredArgsConstructor
public class CustomIntegrationApiService {

    private final ApiClient apiClient;

    /**
     * Creates a new asset by sending a POST request to the asset API.
     *
     * @param assetRequest the asset request payload
     * @return Mono<Asset> representing the created asset
     * @throws WebClientResponseException if the API call fails
     */
    public Mono<Asset> createAsset(OASAssetRequestDataRequest assetRequest) throws WebClientResponseException {
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};

        ParameterizedTypeReference<Asset> localVarReturnType = new ParameterizedTypeReference<Asset>() {
        };
        return apiClient.invokeAPI("/service-api/v2/asset/assets/", HttpMethod.POST, pathParams, queryParams,
                assetRequest,
                headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames,
                localVarReturnType)
            .bodyToMono(localVarReturnType);
    }

    public Mono<OASModelPortfolioResponse> createModelPortfolioRequestCreation(
        List<String> expand, String fields, String omit, OASModelPortfolioRequestDataRequest data, File image)
        throws WebClientResponseException {
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(
            apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)),
                "expand", expand));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "fields", fields));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "omit", omit));

        if (data != null) {
            formParams.add("data", data);
        }
        if (image != null) {
            formParams.add("image", new FileSystemResource(image));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};

        ParameterizedTypeReference<OASModelPortfolioResponse> localVarReturnType = new ParameterizedTypeReference<OASModelPortfolioResponse>() {
        };
        return apiClient.invokeAPI("/integration-api/v2/advice-engines/model-portfolio/model_portfolios/",
                HttpMethod.POST,
                pathParams, queryParams, data, headerParams, cookieParams, formParams, localVarAccept,
                localVarContentType, localVarAuthNames, localVarReturnType)
            .bodyToMono(localVarReturnType);
    }

}
