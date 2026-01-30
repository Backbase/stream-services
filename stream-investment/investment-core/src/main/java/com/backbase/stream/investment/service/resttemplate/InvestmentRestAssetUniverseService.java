package com.backbase.stream.investment.service.resttemplate;

import com.backbase.investment.api.service.sync.ApiClient;
import com.backbase.investment.api.service.sync.v1.AssetUniverseApi;
import com.backbase.investment.api.service.sync.v1.model.AssetCategory;
import com.backbase.investment.api.service.sync.v1.model.OASAssetRequestDataRequest;
import com.backbase.investment.api.service.v1.model.Asset;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class InvestmentRestAssetUniverseService {

    private final AssetUniverseApi assetUniverseApi;
    private final ApiClient apiClient;

    public Mono<Asset> setAssetLogo(Asset asset, Resource logo) {
        String assetUuid = asset.getUuid().toString();

        if (logo == null) {
            log.debug("Skipping logo attachment: assetUuid={}", assetUuid);
            return Mono.just(asset);
        }

        log.info(
            "Starting logo attachment for asset: assetUuid={}, assetName='{}', logoFile='{}'",
            assetUuid, asset.getName(), logo.getFilename());

        return Mono.defer(() -> Mono.just(patchAsset(assetUuid, null, logo)))
            .map(patchedAsset -> {
                log.info(
                    "Logo attached successfully to asset:assetUuid={}, assetName='{}', logoFile='{}'", assetUuid,
                    asset.getName(), logo.getFilename());
                return asset;
            }).onErrorResume(throwable -> {
                log.error(
                    "Logo attachment failed for asset:assetUuid={}, assetName='{}', logoFile='{}', errorType={}, errorMessage={}",
                    assetUuid, asset.getName(), logo.getFilename(), throwable.getClass().getSimpleName(),
                    throwable.getMessage(), throwable);
                log.warn("Asset processing continuing without logo:assetUuid={}", assetUuid);
                return Mono.just(asset);
            });
    }

    public Mono<UUID> setAssetCategoryLogo(UUID assetCategoryId, File logo) {
        String assetCategoryUuid = assetCategoryId.toString();

        if (logo == null) {
            log.debug("Skipping logo attachment: operation=setLogo, assetCategoryUuid={}, reason=noLogoProvided",
                assetCategoryUuid);
            return Mono.empty();
        }

        log.info(
            "Starting logo attachment for asset category: operation=setLogo, assetCategoryUuid={}, logoFile='{}', logoSize={}, action=start",
            assetCategoryUuid, logo.getName(), logo.length());

        return Mono.defer(() -> {
            // verify the required parameter 'uuid' is set
            if (assetCategoryUuid == null) {
                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                    "Missing the required parameter 'uuid' when calling partialUpdateAssetCategory");
            }

            // create path and map variables
            final Map<String, Object> uriVariables = new HashMap<String, Object>();
            uriVariables.put("uuid", assetCategoryUuid);

            final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
            final HttpHeaders localVarHeaderParams = new HttpHeaders();
            final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
            final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

            localVarFormParams.add("image", new FileSystemResource(logo));

            final String[] localVarAccepts = {"application/json"};
            final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
            final String[] localVarContentTypes = {"multipart/form-data"};
            final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

            String[] localVarAuthNames = new String[]{};

            ParameterizedTypeReference<AssetCategory> localReturnType = new ParameterizedTypeReference<AssetCategory>() {
            };
            AssetCategory assetCategory = apiClient.invokeAPI("/service-api/v2/asset/asset-categories/{uuid}/",
                HttpMethod.PATCH, uriVariables, localVarQueryParams, null, localVarHeaderParams,
                localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames,
                localReturnType).getBody();
            return Mono.justOrEmpty(assetCategory.getUuid());
        }).map(patchedAssetCategoryUuId -> {
            log.info(
                "Logo attached successfully to asset category: assetCategoryUuid={}, logoFile='{}'",
                patchedAssetCategoryUuId, logo.getName());
            return patchedAssetCategoryUuId;
        }).onErrorResume(throwable -> {
            log.error(
                "Logo attachment failed for asset category: assetCategoryUuid={}, logoFile='{}', errorType={}, errorMessage={}",
                assetCategoryId, logo.getName(), throwable.getClass().getSimpleName(), throwable.getMessage(),
                throwable);
            log.warn(
                "Asset processing continuing without logo: assetCategoryUuid={}", assetCategoryId);
            return Mono.just(assetCategoryId);
        });
    }

    public com.backbase.investment.api.service.sync.v1.model.Asset patchAsset(String assetIdentifier,
        OASAssetRequestDataRequest data, Resource logo) {
        Object localVarPostBody = null;

        // verify the required parameter 'assetIdentifier' is set
        if (assetIdentifier == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                "Missing the required parameter 'assetIdentifier' when calling patchAsset");
        }

        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("asset_identifier", assetIdentifier);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        if (data != null) {
            localVarFormParams.add("data", data);
        }
        if (logo != null) {
            localVarFormParams.add("logo", logo);
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {
            "multipart/form-data"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};

        ParameterizedTypeReference<com.backbase.investment.api.service.sync.v1.model.Asset> localReturnType = new ParameterizedTypeReference<com.backbase.investment.api.service.sync.v1.model.Asset>() {
        };
        return apiClient.invokeAPI("/service-api/v2/asset/assets/{asset_identifier}/", HttpMethod.PATCH, uriVariables,
                localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams,
                localVarAccept, localVarContentType, localVarAuthNames, localReturnType)
            .getBody();
    }

}