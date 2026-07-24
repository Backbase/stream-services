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
import com.backbase.investment.api.service.sync.v1.model.PatchedEntryTagRequest;
import com.backbase.investment.api.service.sync.v1.model.RelatedAssetSerializerWithAssetCategoriesRequest;
import com.backbase.stream.investment.model.MarketNewsEntry;
import com.backbase.stream.investment.model.ContentTag;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
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
import org.springframework.web.client.RestClientException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * RestTemplate-based service for market news content and tags against the Investment service
 * {@code /service-api/v2/content/} endpoints.
 *
 * <p>This service avoids serialisation issues present in the auto-generated
 * {@code ContentApi#createContentEntry} client introduced with investment-service-api 1.6.x:
 * <ul>
 *   <li>explicit {@code thumbnail: null} is rejected by the investment API</li>
 *   <li>empty {@code assets} arrays must be present on create</li>
 *   <li>multipart create does not reliably transmit JSON array fields</li>
 *   <li>{@code ObjectNode} request bodies are not always serialised by {@code RestTemplate}</li>
 * </ul>
 *
 * <p>Content entry create uses JSON {@code POST /service-api/v2/content/entries/} via
 * {@link ApiClient#invokeAPI}; optional thumbnail upload uses multipart
 * {@code PATCH /service-api/v2/content/entries/{uuid}/}. Mapping from the stream
 * {@link MarketNewsEntry} model is handled by {@link ContentMapper}.
 *
 * @see InvestmentRestDocumentContentService
 */
@Slf4j
@RequiredArgsConstructor
public class InvestmentRestNewsContentService {

    /** Maximum number of content or tag entries retrieved in a single list call. */
    public static final int CONTENT_RETRIEVE_LIMIT = 100;

    private static final String CREATE_CONTENT_ENTRY_PATH = "/service-api/v2/content/entries/";
    private static final String PATCH_CONTENT_ENTRY_PATH = "/service-api/v2/content/entries/{uuid}/";

    private static final String[] JSON_CONTENT_TYPES = {"application/json"};
    private static final String[] MULTIPART_CONTENT_TYPES = {"multipart/form-data"};

    private final ContentApi contentApi;
    private final ApiClient apiClient;
    private final ObjectMapper objectMapper;
    private final ContentMapper contentMapper = Mappers.getMapper(ContentMapper.class);

    /**
     * Creates or updates market news tags via {@code ContentApi} entry-tag endpoints.
     *
     * @param tagEntries tags to upsert
     * @return {@link Mono} that completes when all tags have been processed
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
     * Creates new market news content entries that are not already present in the investment service.
     *
     * @param contentEntries news entries to upsert
     * @return {@link Mono} that completes when all new entries have been processed
     */
    public Mono<Void> upsertContent(List<MarketNewsEntry> contentEntries) {
        log.info("Starting content upsert batch operation: totalEntriesSubmitted={}", contentEntries.size());
        log.debug("Content upsert batch details: entries={}", contentEntries);

        return findEntriesNewContent(contentEntries)
            .flatMap(this::upsertSingleEntry)
            .count()
            .doOnNext(entriesCreated -> log.info(
                "Content upsert batch completed successfully: totalEntriesSubmitted={}, entriesCreated={}",
                contentEntries.size(), entriesCreated))
            .doOnError(error -> log.error(
                "Content upsert batch failed: totalEntriesSubmitted={}, errorType={}, errorMessage={}",
                contentEntries.size(), error.getClass().getSimpleName(), error.getMessage(), error))
            .then();
    }

    private Mono<ContentTag> upsertSingleTag(ContentTag marketNewsTag) {
        log.debug("Processing tag: code='{}', value='{}'", marketNewsTag.getCode(), marketNewsTag.getValue());

        // Validation
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

        // Check if tag entry already exists
        return Mono.fromCallable(() -> contentApi.contentEntryTagList(CONTENT_RETRIEVE_LIMIT, 0))
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

    private Mono<EntryCreateUpdate> upsertSingleEntry(MarketNewsEntry request) {
        log.debug("Creating content entry: title='{}', hasThumbnail={}", request.getTitle(),
            request.getThumbnailResource() != null);

        return Mono.defer(() -> Mono.fromCallable(() -> invokeCreateContentEntry(request)))
            .doOnSuccess(created -> log.info(
                "Content entry created successfully: title='{}', uuid={}, thumbnailAttached={}",
                request.getTitle(), created.getUuid(), request.getThumbnailResource() != null))
            .doOnError(error -> log.error(
                "Content entry creation failed: title='{}', errorType={}, errorMessage={}",
                request.getTitle(), error.getClass().getSimpleName(), error.getMessage(), error))
            .onErrorResume(error -> Mono.empty());
    }

    /**
     * Creates a content entry and optionally attaches a thumbnail file.
     *
     * @throws RestClientException if the investment service returns an error
     */
    private EntryCreateUpdate invokeCreateContentEntry(MarketNewsEntry entry) throws RestClientException {
        EntryCreateUpdateRequest request = contentMapper.map(entry);
        EntryCreateUpdate created = invokeCreate(request);

        Resource thumbnail = entry.getThumbnailResource();
        if (thumbnail != null) {
            return invokePatchThumbnail(created.getUuid(), thumbnail);
        }
        return created;
    }

    private EntryCreateUpdate invokeCreate(EntryCreateUpdateRequest request) throws RestClientException {
        byte[] bodyBytes = buildCreateRequestBodyBytes(request);
        log.debug("Creating content entry JSON payload: title='{}'", request.getTitle());

        final List<MediaType> accept = apiClient.selectHeaderAccept(new String[]{"application/json"});
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

    private EntryCreateUpdate invokePatchThumbnail(UUID uuid, Resource thumbnail) throws RestClientException {
        final Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("uuid", uuid.toString());

        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();
        log.debug("Patching content entry thumbnail: uuid={}, thumbnailFile='{}'",
            uuid, getFileNameForLog(thumbnail));
        formParams.add(JSON_PROPERTY_THUMBNAIL, thumbnail);

        final List<MediaType> accept = apiClient.selectHeaderAccept(new String[]{"application/json"});
        final MediaType contentType = apiClient.selectHeaderContentType(MULTIPART_CONTENT_TYPES);
        ParameterizedTypeReference<EntryCreateUpdate> returnType = new ParameterizedTypeReference<>() {
        };

        return apiClient.invokeAPI(
                PATCH_CONTENT_ENTRY_PATH,
                HttpMethod.PATCH,
                uriVariables,
                new LinkedMultiValueMap<>(),
                null,
                new HttpHeaders(),
                new LinkedMultiValueMap<>(),
                formParams,
                accept,
                contentType,
                new String[]{},
                returnType)
            .getBody();
    }

    /**
     * Builds JSON request bytes for content entry create.
     *
     * <p>{@code thumbnail} is omitted (investment rejects explicit null) and {@code assets} is always
     * included, even when empty. The payload is serialised to {@code byte[]} because
     * {@code RestTemplate} does not reliably serialise {@link ObjectNode} bodies.
     */
    private byte[] buildCreateRequestBodyBytes(EntryCreateUpdateRequest request) {
        ObjectNode requestBody = objectMapper.valueToTree(request);
        requestBody.remove(JSON_PROPERTY_THUMBNAIL);
        requestBody.set(JSON_PROPERTY_ASSETS, objectMapper.valueToTree(
            Objects.requireNonNullElse(request.getAssets(), List.of())));

        try {
            return objectMapper.writeValueAsBytes(requestBody);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize content entry create request", exception);
        }
    }

    private Flux<MarketNewsEntry> findEntriesNewContent(List<MarketNewsEntry> contentEntries) {
        Map<String, MarketNewsEntry> entryByTitle = contentEntries.stream()
            .collect(Collectors.toMap(MarketNewsEntry::getTitle, Function.identity()));
        log.debug("Filtering content entries: requestedTitles={}", entryByTitle.keySet());

        List<Entry> existingNews = contentApi.listContentEntries(null, CONTENT_RETRIEVE_LIMIT, 0, null, null, null, null)
            .getResults().stream().filter(Objects::nonNull).toList();

        if (existingNews.isEmpty()) {
            log.info(
                "No existing content found in system: totalEntriesSubmitted={}, existingEntries=0, entriesToCreate={}",
                entryByTitle.size(), entryByTitle.size());
            return Flux.fromIterable(entryByTitle.values());
        }

        Set<String> existingTitles = existingNews.stream().map(Entry::getTitle).collect(Collectors.toSet());
        List<MarketNewsEntry> newEntries = contentEntries.stream()
            .filter(entry -> existingTitles.stream().noneMatch(existingTitle -> entry.getTitle().contains(existingTitle)))
            .toList();

        log.info(
            "Content filtering completed: totalEntriesSubmitted={}, existingEntriesFound={}, "
                + "entriesToCreate={}, duplicatesSkipped={}",
            entryByTitle.size(), existingNews.size(), newEntries.size(), entryByTitle.size() - newEntries.size());
        log.debug("Filtered new content titles: titles={}",
            newEntries.stream().map(MarketNewsEntry::getTitle).collect(Collectors.toList()));

        return Flux.fromIterable(newEntries);
    }
}
