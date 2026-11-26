package com.backbase.stream.investment.service.resttemplate;

import com.backbase.investment.api.service.sync.ApiClient;
import static com.backbase.investment.api.service.sync.v1.model.AssetCategoryRequest.JSON_PROPERTY_TRANSLATIONS;

import com.backbase.investment.api.service.sync.v1.model.AssetCategory;
import com.backbase.investment.api.service.sync.v1.model.AssetCategoryRequest;
import com.backbase.investment.api.service.sync.v1.model.OASAssetRequestDataRequest;
import com.backbase.investment.api.service.sync.v1.model.PatchedAssetCategoryRequest;
import com.backbase.investment.api.service.v1.model.Asset;
import com.backbase.stream.configuration.IngestConfigProperties;
import com.backbase.stream.investment.TranslationsSupport;
import com.backbase.stream.investment.model.AssetCategoryEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.core.ParameterizedTypeReference;
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

    private final ApiClient apiClient;
    private final IngestConfigProperties ingestProperties;
    private final RestTemplateAssetMapper assetMapper = Mappers.getMapper(RestTemplateAssetMapper.class);

    public Mono<com.backbase.stream.investment.Asset> createAsset(com.backbase.stream.investment.Asset asset,
        Map<String, UUID> categoryIdByCode) {

        OASAssetRequestDataRequest assetRequestDataRequest = assetMapper.mapAsset(asset, categoryIdByCode);
        applyAssetTranslations(assetRequestDataRequest, asset);

        log.info(
            "Start created asset with assetIdentifier:: assetUuid={}, assetName='{}', logoFile='{}'",
            asset.getKeyString(), asset.getName(), asset.getLogo());

        return Mono.defer(() -> Mono.just(createAsset(assetRequestDataRequest, asset.getLogoFile())))
            .map(createdAsset -> {
                log.info(
                    "Asset created successfully: assetUuid={}, assetName='{}'", createdAsset.getUuid(),
                    createdAsset.getName());
                return assetMapper.mapFromSyncAsset(createdAsset);
            })
            .onErrorResume(throwable -> {
                log.error(
                    "Asset creation failed for asset:asset={}, assetName='{}', logoFile='{}', errorType={}, errorMessage={}",
                    asset.getKeyString(), asset.getName(), asset.getLogo(), throwable.getClass().getSimpleName(),
                    throwable.getMessage(), throwable);
                log.warn("Asset processing continuing without asset={}", asset.getKeyString());
                return Mono.just(asset);
            });
    }

    public Mono<com.backbase.stream.investment.Asset> patchAsset(Asset existAsset,
        com.backbase.stream.investment.Asset asset, Map<String, UUID> categoryIdByCode) {
        String assetUuid = existAsset.getUuid().toString();

        log.info(
            "Starting asset update: assetUuid={}, assetName='{}', logoFile='{}'",
            assetUuid, asset.getName(), asset.getLogo());
        OASAssetRequestDataRequest assetRequestDataRequest = assetMapper.mapAsset(asset, categoryIdByCode);
        applyAssetTranslations(assetRequestDataRequest, asset);
        return Mono.defer(() -> Mono.just(patchAsset(assetUuid, assetRequestDataRequest, asset.getLogoFile())))
            .map(patchedAsset -> {
                log.info(
                    "Logo attached successfully to asset:assetUuid={}, assetName='{}', logoFile='{}'", assetUuid,
                    asset.getName(), asset.getLogo());
                return asset;
            }).onErrorResume(throwable -> {
                log.error(
                    "Logo attachment failed for asset:assetUuid={}, assetName='{}', logoFile='{}', errorType={}, errorMessage={}",
                    assetUuid, asset.getName(), asset.getLogo(), throwable.getClass().getSimpleName(),
                    throwable.getMessage(), throwable);
                log.warn("Asset processing continuing without logo:assetUuid={}", assetUuid);
                return Mono.just(asset);
            });
    }

    private com.backbase.investment.api.service.sync.v1.model.Asset createAsset(OASAssetRequestDataRequest data,
        Resource logo) {
        Object localVarPostBody = null;

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        if (data != null) {
            localVarFormParams.add("data", data);
        }
        if (ingestProperties.getAsset().isIngestImages() && logo != null) {
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
        return apiClient.invokeAPI("/service-api/v2/asset/assets/", HttpMethod.POST,
                Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams,
                localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames,
                localReturnType)
            .getBody();
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
        if (ingestProperties.getAsset().isIngestImages() && logo != null) {
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

    public Mono<UUID> setAssetCategoryLogo(UUID assetCategoryId, Resource logo) {
        String assetCategoryUuid = assetCategoryId.toString();

        if (logo == null) {
            log.debug("Skipping logo attachment: operation=setLogo, assetCategoryUuid={}, reason=noLogoProvided",
                assetCategoryUuid);
            return Mono.empty();
        }

        log.info(
            "Starting logo attachment for asset category: operation=setLogo, assetCategoryUuid={}, logoFile='{}'",
            assetCategoryUuid, getFileNameForLog(logo));

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

            if (ingestProperties.getAsset().isIngestImages()) {
                localVarFormParams.add("image", logo);
            }

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
                patchedAssetCategoryUuId, getFileNameForLog(logo));
            return patchedAssetCategoryUuId;
        }).onErrorResume(throwable -> {
            log.error(
                "Logo attachment failed for asset category: assetCategoryUuid={}, logoFile='{}', errorType={}, errorMessage={}",
                assetCategoryId, getFileNameForLog(logo), throwable.getClass().getSimpleName(), throwable.getMessage(),
                throwable);
            log.warn(
                "Asset processing continuing without logo: assetCategoryUuid={}", assetCategoryId);
            return Mono.just(assetCategoryId);
        });
    }

    public Mono<AssetCategory> patchAssetCategory(UUID assetCategoryId,
        AssetCategoryEntry assetCategoryEntry, Resource image) {

        PatchedAssetCategoryRequest assetCategoryPatch = assetMapper.mapPatchAssetCategory(
            assetCategoryEntry);

        String uuid = assetCategoryId.toString();

        log.info(
            "Starting asset category patch: assetCategoryUuid={}, logoFile='{}'",
            uuid, getFileNameForLog(image));

        return Mono.defer(() -> {
            if (uuid == null) {
                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                    "Missing the required parameter 'uuid' when calling partialUpdateAssetCategory");
            }

            // create path and map variables
            final Map<String, Object> uriVariables = new HashMap<String, Object>();
            uriVariables.put("uuid", uuid);

            final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
            final HttpHeaders localVarHeaderParams = new HttpHeaders();
            final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
            final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

            final boolean multipart = useMultipartForCategory(image);
            final Object requestBody;
            final String[] localVarContentTypes;
            if (multipart) {
                Optional.ofNullable(assetCategoryPatch.getName())
                    .ifPresent(v -> localVarFormParams.add("name", v));
                Optional.ofNullable(assetCategoryPatch.getCode())
                    .ifPresent(v -> localVarFormParams.add("code", v));
                Optional.ofNullable(assetCategoryPatch.getOrder())
                    .ifPresent(v -> localVarFormParams.add("order", v));
                Optional.ofNullable(assetCategoryPatch.getType())
                    .ifPresent(v -> localVarFormParams.add("type", v));
                Optional.ofNullable(assetCategoryPatch.getExcerpt())
                    .ifPresent(v -> localVarFormParams.add("excerpt", v));
                Optional.ofNullable(assetCategoryPatch.getDescription())
                    .ifPresent(v -> localVarFormParams.add("description", v));
                applyCategoryTranslationsToForm(localVarFormParams, assetCategoryEntry);
                localVarFormParams.add("image", image);
                requestBody = null;
                localVarContentTypes = new String[]{"multipart/form-data"};
            } else {
                assetCategoryPatch.setTranslations(filteredCategoryTranslations(assetCategoryEntry));
                requestBody = assetCategoryPatch;
                localVarContentTypes = new String[]{"application/json"};
            }

            final String[] localVarAccepts = {
                "application/json"
            };
            final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
            final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

            String[] localVarAuthNames = new String[]{};

            ParameterizedTypeReference<AssetCategory> localReturnType = new ParameterizedTypeReference<AssetCategory>() {
            };
            return Mono.just(
                apiClient.invokeAPI("/service-api/v2/asset/asset-categories/{uuid}/", HttpMethod.PATCH, uriVariables,
                        localVarQueryParams, requestBody, localVarHeaderParams, localVarCookieParams, localVarFormParams,
                        localVarAccept, localVarContentType, localVarAuthNames, localReturnType)
                    .getBody());
        });
    }

    @Nonnull
    public static String getFileNameForLog(Resource image) {
        return Optional.ofNullable(image).map(Resource::getFilename).orElse("null");
    }

    public Mono<AssetCategory> createAssetCategory(AssetCategoryEntry assetCategoryEntry, Resource image) {

        AssetCategoryRequest assetCategoryRequest = assetMapper.mapAssetCategory(assetCategoryEntry);

        log.info(
            "Starting create asset category : assetCategory={}, logoFile='{}'",
            assetCategoryRequest.getName(), getFileNameForLog(image));

        // verify the required parameter 'assetCategoryRequest' is set
        if (assetCategoryRequest == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                "Missing the required parameter 'assetCategoryRequest' when calling createAssetCategory");
        }

        return Mono.defer(() -> {
            // create path and map variables
            final Map<String, Object> uriVariables = new HashMap<String, Object>();

            final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
            final HttpHeaders localVarHeaderParams = new HttpHeaders();
            final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
            final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

            final boolean multipart = useMultipartForCategory(image);
            final Object requestBody;
            final String[] localVarContentTypes;
            if (multipart) {
                Optional.ofNullable(assetCategoryRequest.getName())
                    .ifPresent(v -> localVarFormParams.add("name", v));
                Optional.ofNullable(assetCategoryRequest.getCode())
                    .ifPresent(v -> localVarFormParams.add("code", v));
                Optional.ofNullable(assetCategoryRequest.getOrder())
                    .ifPresent(v -> localVarFormParams.add("order", v));
                Optional.ofNullable(assetCategoryRequest.getType())
                    .ifPresent(v -> localVarFormParams.add("type", v));
                Optional.ofNullable(assetCategoryRequest.getExcerpt())
                    .ifPresent(v -> localVarFormParams.add("excerpt", v));
                Optional.ofNullable(assetCategoryRequest.getDescription())
                    .ifPresent(v -> localVarFormParams.add("description", v));
                applyCategoryTranslationsToForm(localVarFormParams, assetCategoryEntry);
                localVarFormParams.add("image", image);
                requestBody = null;
                localVarContentTypes = new String[]{"multipart/form-data"};
            } else {
                assetCategoryRequest.setTranslations(filteredCategoryTranslations(assetCategoryEntry));
                requestBody = assetCategoryRequest;
                localVarContentTypes = new String[]{"application/json"};
            }

            final String[] localVarAccepts = {
                "application/json"
            };
            final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
            final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

            String[] localVarAuthNames = new String[]{};

            ParameterizedTypeReference<AssetCategory> localReturnType = new ParameterizedTypeReference<AssetCategory>() {
            };
            return Mono.just(apiClient.invokeAPI("/service-api/v2/asset/asset-categories/", HttpMethod.POST,
                    uriVariables,
                    localVarQueryParams, requestBody, localVarHeaderParams, localVarCookieParams, localVarFormParams,
                    localVarAccept, localVarContentType, localVarAuthNames, localReturnType)
                .getBody());
        });

    }

    /**
     * Investment accepts category {@code translations} as a nested object. Flat multipart without an image
     * does not encode maps correctly; use JSON on the OpenAPI request DTO instead (Jackson on {@link ApiClient}'s
     * RestTemplate). With an image in the same request, use multipart and the same filtered {@link Map} as portfolio products.
     */
    private boolean useMultipartForCategory(Resource image) {
        return ingestProperties.getAsset().isIngestImages() && image != null;
    }

    private static Map<String, Map<String, String>> filteredCategoryTranslations(AssetCategoryEntry entry) {
        return TranslationsSupport.filterTranslations(entry.getTranslations());
    }

    private static void applyCategoryTranslationsToForm(MultiValueMap<String, Object> formParams,
        AssetCategoryEntry entry) {
        Optional.ofNullable(filteredCategoryTranslations(entry))
            .ifPresent(v -> formParams.add(JSON_PROPERTY_TRANSLATIONS, v));
    }

    private void applyAssetTranslations(OASAssetRequestDataRequest request,
        com.backbase.stream.investment.Asset asset) {
        request.setTranslations(TranslationsSupport.filterTranslations(asset.getTranslations()));
    }

}