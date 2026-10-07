package com.backbase.stream.investment.service.resttemplate;

import static com.backbase.investment.api.service.sync.v1.model.EntryCreateUpdateRequest.JSON_PROPERTY_ASSETS;
import static com.backbase.investment.api.service.sync.v1.model.EntryCreateUpdateRequest.JSON_PROPERTY_THUMBNAIL;
import static com.backbase.stream.investment.service.resttemplate.InvestmentRestAssetUniverseService.getFileNameForLog;

import com.backbase.investment.api.service.sync.ApiClient;
import com.backbase.investment.api.service.sync.v1.ContentApi;
import com.backbase.investment.api.service.sync.v1.model.Entry;
import com.backbase.investment.api.service.sync.v1.model.EntryCreateUpdate;
import com.backbase.investment.api.service.sync.v1.model.EntryCreateUpdateRequest;
import com.backbase.investment.api.service.sync.v1.model.EntryTagRequest;
import com.backbase.investment.api.service.sync.v1.model.PatchedEntryCreateUpdateRequest;
import com.backbase.investment.api.service.sync.v1.model.PatchedEntryTagRequest;
import com.backbase.stream.investment.model.MarketNewsEntry;
import com.backbase.stream.investment.model.ContentTag;
import com.backbase.stream.investment.model.UpsertPartition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST client service for upserting market news content and tags via the Investment Content API.
 *
 * <p>This service manages:
 * <ul>
 *   <li>Market news tag creation and updates</li>
 *   <li>Market news content entry create and update (matched by {@code external_id}, then title)</li>
 *   <li>Thumbnail attachment for newly created content entries</li>
 * </ul>
 *
 * <p>Content entry create uses JSON {@code POST /service-api/v2/content/entries/} via
 * {@link ApiClient#invokeAPI} instead of the generated {@code ContentApi#createContentEntry},
 * because investment-service-api 1.6.x rejects explicit {@code thumbnail: null}, requires an
 * {@code assets} array (including empty), and multipart create does not reliably transmit JSON
 * array fields. Thumbnail upload remains a multipart PATCH.
 *
 * <p>Design notes (see CODING_RULES_COPILOT.md):
 * <ul>
 *   <li>No direct manipulation of generated API classes beyond construction and mapping</li>
 *   <li>Side-effecting operations are logged at info (create) or debug (patch) levels</li>
 *   <li>Individual entry failures are logged and swallowed so batch processing continues</li>
 *   <li>All reactive operations include proper success and error handlers for observability</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class InvestmentRestNewsContentService {

    /** Maximum number of content or tag entries retrieved in a single list call. */
    public static final int CONTENT_RETRIEVE_LIMIT = 100;

    private static final String CREATE_CONTENT_ENTRY_PATH = "/service-api/v2/content/entries/";
    private static final String PATCH_CONTENT_ENTRY_PATH = "/service-api/v2/content/entries/{uuid}/";
    private static final String[] JSON_CONTENT_TYPES = {"application/json"};

    private final ContentApi contentApi;
    private final ApiClient apiClient;
    private final ObjectMapper objectMapper;
    private final ContentMapper contentMapper = Mappers.getMapper(ContentMapper.class);

    /**
     * Upserts a batch of content tags. For each tag, checks whether a tag with the same code already exists.
     * If found, patches it; otherwise creates a new tag. Tags with blank code or value are skipped.
     * Individual failures are logged and swallowed so remaining tags continue processing.
     *
     * @param tagEntries list of tags to upsert
     * @return Mono that completes when all tags have been processed
     */
    public Mono<Void> upsertTags(List<ContentTag> tagEntries) {
        log.info("Starting tag upsert batch operation: totalEntriesSubmitted={}", tagEntries.size());
        log.debug("Tag upsert batch details: entries={}", tagEntries);

        return Flux.fromIterable(tagEntries)
            .flatMap(this::upsertSingleTag)
            .count()
            .doOnNext(processedCount -> log.info(
                "Tag upsert batch completed successfully: totalEntriesSubmitted={}, tagsUpserted={}",
                tagEntries.size(), processedCount))
            .doOnError(error -> log.error(
                "Tag upsert batch failed: totalEntriesSubmitted={}, errorType={}, errorMessage={}",
                tagEntries.size(), error.getClass().getSimpleName(), error.getMessage(), error))
            .then();
    }

    /**
     * Upserts a single tag entry using the ContentApi tag endpoints. Implementation follows the upsert pattern:
     * <ol>
     *   <li>List existing tag entries to check if the tag code already exists</li>
     *   <li>If tag exists, patch it with the new value</li>
     *   <li>If not found, create a new tag entry</li>
     * </ol>
     *
     * @param marketNewsTag the tag to upsert
     * @return Mono that completes with the tag when processed, or empty if validation fails or an error occurs
     */
    private Mono<ContentTag> upsertSingleTag(ContentTag marketNewsTag) {
        log.debug("Processing tag: code='{}', value='{}'", marketNewsTag.getCode(), marketNewsTag.getValue());

        if (marketNewsTag.getCode() == null || marketNewsTag.getCode().isBlank()) {
            log.warn("Skipping tag with empty code: value='{}'", marketNewsTag.getValue());
            return Mono.empty();
        }

        if (marketNewsTag.getValue() == null || marketNewsTag.getValue().isBlank()) {
            log.warn("Skipping tag with empty value: code='{}'", marketNewsTag.getCode());
            return Mono.empty();
        }

        log.debug("Checking if tag entry exists: code='{}', value='{}'",
            marketNewsTag.getCode(), marketNewsTag.getValue());

        return Mono.fromCallable(() ->
                contentApi.contentEntryTagList(CONTENT_RETRIEVE_LIMIT, 0))
            .map(paginatedList -> paginatedList.getResults().stream()
                .filter(Objects::nonNull)
                .filter(entry -> marketNewsTag.getCode().equals(entry.getCode()))
                .findFirst())
            .flatMap(existingEntry -> {
                if (existingEntry.isPresent()) {
                    log.debug("Patching existing tag entry: code='{}', value='{}'",
                        marketNewsTag.getCode(), marketNewsTag.getValue());
                    return patchTagEntry(marketNewsTag);
                }
                log.debug("Creating new tag entry: code='{}', value='{}'",
                    marketNewsTag.getCode(), marketNewsTag.getValue());
                return createTagEntry(marketNewsTag);
            })
            .doOnError(error -> log.error(
                "Tag upsert failed: code='{}', value='{}', errorType={}, errorMessage={}",
                marketNewsTag.getCode(), marketNewsTag.getValue(),
                error.getClass().getSimpleName(), error.getMessage(), error))
            .onErrorResume(error -> {
                log.warn("Continuing without tag: code='{}', reason={}",
                    marketNewsTag.getCode(), error.getMessage());
                return Mono.empty();
            });
    }

    /**
     * Creates a new tag entry using the ContentApi.
     *
     * @param contentTag the tag to create an entry for
     * @return Mono of the created tag
     */
    private Mono<ContentTag> createTagEntry(ContentTag contentTag) {
        EntryTagRequest request = new EntryTagRequest()
            .code(contentTag.getCode())
            .value(contentTag.getValue());

        return Mono.defer(() -> Mono.just(contentApi.contentEntryTagCreate(request)))
            .doOnSuccess(created -> log.info(
                "Tag entry created successfully: code='{}', value='{}'",
                created.getCode(), created.getValue()))
            .doOnError(error -> log.error(
                "Tag entry creation failed: code='{}', value='{}', errorType={}, errorMessage={}",
                contentTag.getCode(), contentTag.getValue(),
                error.getClass().getSimpleName(), error.getMessage(), error))
            .thenReturn(contentTag);
    }

    /**
     * Patches an existing tag entry with updated values.
     *
     * @param contentTag the tag with updated values to patch
     * @return Mono of the patched tag
     */
    private Mono<ContentTag> patchTagEntry(ContentTag contentTag) {
        PatchedEntryTagRequest request = new PatchedEntryTagRequest()
            .code(contentTag.getCode())
            .value(contentTag.getValue());

        return Mono.defer(() -> Mono.just(contentApi.contentEntryTagPartialUpdate(contentTag.getCode(), request)))
            .doOnSuccess(patched -> log.debug(
                "Tag entry patched successfully: code='{}', value='{}'",
                patched.getCode(), patched.getValue()))
            .doOnError(error -> log.error(
                "Tag entry patch failed: code='{}', value='{}', errorType={}, errorMessage={}",
                contentTag.getCode(), contentTag.getValue(),
                error.getClass().getSimpleName(), error.getMessage(), error))
            .thenReturn(contentTag);
    }

    /**
     * Creates or updates market news content entries matched by {@code external_id}, then by title.
     */
    public Mono<Void> upsertContent(List<MarketNewsEntry> contentEntries) {
        log.info("Starting content entries upsert batch operation: totalEntriesSubmitted={}", contentEntries.size());
        log.debug("Content upsert batch details: entries={}", contentEntries);

        return findUpsertEntries(contentEntries)
            .flatMap(this::upsertSingleEntry)
            .count()
            .doOnNext(entriesProcessed -> log.info(
                "Content upsert batch completed successfully: totalEntriesSubmitted={}, entriesProcessed={}",
                contentEntries.size(), entriesProcessed))
            .doOnError(error -> log.error(
                "Content upsert batch failed: totalEntriesSubmitted={}, errorType={}, errorMessage={}",
                contentEntries.size(), error.getClass().getSimpleName(), error.getMessage(), error))
            .then();
    }

    /**
     * Creates or patches a single market news content entry from an upsert partition produced by
     * {@link #findUpsertEntries(List)}.
     *
     * <p>When {@link UpsertPartition#id()} is {@code null}, delegates to {@link #createSingleEntry(MarketNewsEntry)};
     * otherwise patches the entry at that UUID via {@link #patchSingleEntry(UUID, MarketNewsEntry)}. Both paths
     * optionally attach a thumbnail. Failures are logged and swallowed in those delegates so batch processing
     * continues.
     *
     * @param partition existing entry UUID (when matched by {@code external_id} or title) and payload to upsert
     * @return Mono that completes with the created or patched entry, or empty if the operation fails
     */
    private Mono<EntryCreateUpdate> upsertSingleEntry(UpsertPartition<UUID, MarketNewsEntry> partition) {
        MarketNewsEntry request = partition.entity();
        if (partition.id() == null) {
            return createSingleEntry(request);
        }
        return patchSingleEntry(partition.id(), request);
    }

    private Mono<EntryCreateUpdate> createSingleEntry(MarketNewsEntry request) {
        log.debug("Creating content entry: externalId={}, title='{}', hasThumbnail={}",
            request.getExternalId(), request.getTitle(), request.getThumbnailResource() != null);

        EntryCreateUpdateRequest createUpdateRequest = contentMapper.map(request);

        return Mono.defer(() -> Mono.fromCallable(() -> createContentEntry(createUpdateRequest)))
            .flatMap(entry -> addThumbnail(entry, request.getThumbnailResource()))
            .doOnSuccess(created -> log.info(
                "Content entry created successfully: externalId={}, title='{}', uuid={}",
                request.getExternalId(), request.getTitle(), created.getUuid()))
            .doOnError(error -> log.error(
                "Content entry creation failed: externalId={}, title='{}', errorType={}, errorMessage={}",
                request.getExternalId(), request.getTitle(), error.getClass().getSimpleName(), error.getMessage(),
                error))
            .onErrorResume(error -> Mono.empty());
    }

    private Mono<EntryCreateUpdate> patchSingleEntry(UUID uuid, MarketNewsEntry request) {
        log.debug("Patching content entry: uuid={}, externalId={}, title='{}'",
            uuid, request.getExternalId(), request.getTitle());

        PatchedEntryCreateUpdateRequest patchRequest = contentMapper.mapPatch(request);

        return Mono.defer(() -> Mono.fromCallable(() -> patchContentEntry(uuid, patchRequest)))
            .flatMap(entry -> addThumbnail(entry, request.getThumbnailResource()))
            .doOnSuccess(patched -> log.info(
                "Content entry patched successfully: externalId={}, title='{}', uuid={}",
                request.getExternalId(), request.getTitle(), patched.getUuid()))
            .doOnError(error -> log.error(
                "Content entry patch failed: uuid={}, externalId={}, title='{}', errorType={}, errorMessage={}",
                uuid, request.getExternalId(), request.getTitle(), error.getClass().getSimpleName(),
                error.getMessage(), error))
            .onErrorResume(error -> Mono.empty());
    }

    /**
     * Creates a content entry via JSON POST.
     *
     * <p>{@code thumbnail} is omitted (investment rejects explicit null) and {@code assets} is always
     * included, even when empty. The payload is serialised to {@code byte[]} because
     * {@code RestTemplate} does not reliably serialise {@link ObjectNode} bodies.
     */
    private EntryCreateUpdate createContentEntry(EntryCreateUpdateRequest request) {
        ObjectNode requestBody = objectMapper.valueToTree(request);
        requestBody.remove(JSON_PROPERTY_THUMBNAIL);
        requestBody.set(JSON_PROPERTY_ASSETS, objectMapper.valueToTree(
            Objects.requireNonNullElse(request.getAssets(), List.of())));

        final byte[] bodyBytes;
        try {
            bodyBytes = objectMapper.writeValueAsBytes(requestBody);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize content entry create request", exception);
        }

        final List<MediaType> accept = apiClient.selectHeaderAccept(JSON_CONTENT_TYPES);
        final MediaType contentType = apiClient.selectHeaderContentType(JSON_CONTENT_TYPES);
        ParameterizedTypeReference<EntryCreateUpdate> returnType = new ParameterizedTypeReference<>() {
        };

        return apiClient.invokeAPI(
                CREATE_CONTENT_ENTRY_PATH,
                HttpMethod.POST,
                Collections.emptyMap(),
                new LinkedMultiValueMap<>(),
                bodyBytes,
                new HttpHeaders(),
                new LinkedMultiValueMap<>(),
                new LinkedMultiValueMap<>(),
                accept,
                contentType,
                new String[]{},
                returnType)
            .getBody();
    }

    /**
     * Patches a content entry via JSON PATCH.
     *
     * <p>Uses the same payload rules as {@link #createContentEntry}: omit {@code thumbnail} (investment rejects
     * explicit null) and always send an {@code assets} array.
     */
    private EntryCreateUpdate patchContentEntry(UUID uuid, PatchedEntryCreateUpdateRequest request) {
        ObjectNode requestBody = objectMapper.valueToTree(request);
        requestBody.remove(JSON_PROPERTY_THUMBNAIL);
        requestBody.set(JSON_PROPERTY_ASSETS, objectMapper.valueToTree(
            Objects.requireNonNullElse(request.getAssets(), List.of())));

        final byte[] bodyBytes;
        try {
            bodyBytes = objectMapper.writeValueAsBytes(requestBody);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize content entry patch request", exception);
        }

        final Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("uuid", uuid.toString());

        final List<MediaType> accept = apiClient.selectHeaderAccept(JSON_CONTENT_TYPES);
        final MediaType contentType = apiClient.selectHeaderContentType(JSON_CONTENT_TYPES);
        ParameterizedTypeReference<EntryCreateUpdate> returnType = new ParameterizedTypeReference<>() {
        };

        return apiClient.invokeAPI(
                PATCH_CONTENT_ENTRY_PATH,
                HttpMethod.PATCH,
                uriVariables,
                new LinkedMultiValueMap<>(),
                bodyBytes,
                new HttpHeaders(),
                new LinkedMultiValueMap<>(),
                new LinkedMultiValueMap<>(),
                accept,
                contentType,
                new String[]{},
                returnType)
            .getBody();
    }

    private Flux<UpsertPartition<UUID, MarketNewsEntry>> findUpsertEntries(List<MarketNewsEntry> contentEntries) {
        List<Entry> existingEntries = contentApi.listContentEntries(null, CONTENT_RETRIEVE_LIMIT, 0, null, null, null,
                null)
            .getResults().stream().filter(Objects::nonNull).toList();

        if (existingEntries.isEmpty()) {
            log.info("No existing content entries in system: entriesToCreate={}", contentEntries.size());
            return Flux.fromIterable(contentEntries.stream()
                .map(UpsertPartition::<UUID, MarketNewsEntry>createPartition)
                .toList());
        }

        Map<String, UUID> byExternalId = existingEntries.stream()
            .filter(entry -> StringUtils.hasText(entry.getExternalId()))
            .collect(Collectors.toMap(Entry::getExternalId, Entry::getUuid, (existing, replacement) -> existing));
        Map<String, UUID> byTitle = existingEntries.stream()
            .collect(Collectors.toMap(Entry::getTitle, Entry::getUuid, (existing, replacement) -> existing));

        List<UpsertPartition<UUID, MarketNewsEntry>> partitions = contentEntries.stream()
            .map(entry -> {
                UUID existingUuid = null;
                if (StringUtils.hasText(entry.getExternalId())) {
                    existingUuid = byExternalId.get(entry.getExternalId());
                }
                if (existingUuid == null) {
                    existingUuid = byTitle.get(entry.getTitle());
                }
                return new UpsertPartition<>(existingUuid, entry);
            })
            .toList();

        log.info("Content entry upsert plan: submitted={}, existingInService={}, toCreate={}, toPatch={}",
            contentEntries.size(), existingEntries.size(),
            partitions.stream().filter(p -> p.id() == null).count(),
            partitions.stream().filter(p -> p.id() != null).count());

        return Flux.fromIterable(partitions);
    }

    /**
     * Attaches a thumbnail to a content entry via multipart PATCH when a thumbnail resource is provided.
     * Failures are logged and swallowed so the created entry is retained without a thumbnail.
     *
     * @param entry the created content entry
     * @param thumbnail optional thumbnail resource
     * @return Mono emitting the entry (unchanged on success or when attachment is skipped or fails)
     */
    private Mono<EntryCreateUpdate> addThumbnail(EntryCreateUpdate entry, Resource thumbnail) {
        UUID uuid = entry.getUuid();

        if (thumbnail == null) {
            log.debug("Skipping thumbnail attachment: uuid={}, title='{}'", uuid, entry.getTitle());
            return Mono.just(entry);
        }

        log.debug("Attaching thumbnail to content entry: uuid={}, title='{}', thumbnailFile='{}'", uuid,
            entry.getTitle(), getFileNameForLog(thumbnail));

        return Mono.defer(() -> {
                Map<String, Object> uriVariables = new HashMap<>();
                uriVariables.put("uuid", uuid);

                MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<>();
                HttpHeaders localVarHeaderParams = new HttpHeaders();
                MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<>();
                MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<>();

                localVarFormParams.add(JSON_PROPERTY_THUMBNAIL, thumbnail);

                final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(JSON_CONTENT_TYPES);
                final String[] localVarContentTypes = {"multipart/form-data"};
                final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

                String[] localVarAuthNames = new String[]{};

                ParameterizedTypeReference<EntryCreateUpdate> localReturnType = new ParameterizedTypeReference<>() {
                };
                apiClient.invokeAPI(PATCH_CONTENT_ENTRY_PATH, HttpMethod.PATCH, uriVariables,
                    localVarQueryParams, null, localVarHeaderParams, localVarCookieParams, localVarFormParams,
                    localVarAccept, localVarContentType, localVarAuthNames, localReturnType);

                log.debug("Thumbnail attached successfully: uuid={}, title='{}', thumbnailFile='{}'", uuid,
                    entry.getTitle(), getFileNameForLog(thumbnail));
                return Mono.just(entry);
            }).doOnError(error -> log.error(
                "Thumbnail attachment failed: uuid={}, title='{}', thumbnailFile='{}', errorType={}, errorMessage={}",
                uuid, entry.getTitle(), getFileNameForLog(thumbnail), error.getClass().getSimpleName(),
                error.getMessage(), error))
            .onErrorResume(error -> {
                log.warn("Content entry created without thumbnail: uuid={}, title='{}', reason={}", uuid,
                    entry.getTitle(), error.getMessage());
                return Mono.just(entry);
            });
    }

}
