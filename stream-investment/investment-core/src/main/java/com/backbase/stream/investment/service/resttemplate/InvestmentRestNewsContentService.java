package com.backbase.stream.investment.service.resttemplate;

import static com.backbase.stream.investment.service.resttemplate.InvestmentRestAssetUniverseService.getFileNameForLog;

import com.backbase.investment.api.service.sync.ApiClient;
import com.backbase.investment.api.service.sync.v1.ContentApi;
import com.backbase.investment.api.service.sync.v1.model.Entry;
import com.backbase.investment.api.service.sync.v1.model.EntryCreateUpdate;
import com.backbase.investment.api.service.sync.v1.model.EntryCreateUpdateRequest;
import com.backbase.investment.api.service.sync.v1.model.EntryTagRequest;
import com.backbase.investment.api.service.sync.v1.model.PatchedEntryTagRequest;
import com.backbase.stream.investment.model.MarketNewsEntry;
import com.backbase.stream.investment.model.ContentTag;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST client service for upserting market news content and tags via the Investment Content API.
 *
 * <p>This service manages:
 * <ul>
 *   <li>Market news tag creation and updates</li>
 *   <li>Market news content entry creation (skips duplicates by title)</li>
 *   <li>Thumbnail attachment for newly created content entries</li>
 * </ul>
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
    private final ContentApi contentApi;
    private final ApiClient apiClient;
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
     * Creates new market news content entries. Entries whose title matches an existing entry are skipped.
     * Individual failures are logged and swallowed so remaining entries continue processing.
     *
     * @param contentEntries list of content entries to create
     * @return Mono that completes when all eligible entries have been processed
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

    /**
     * Creates a single market news content entry and optionally attaches a thumbnail.
     * Callers must supply entries that have already been filtered as non-duplicates.
     * Errors are logged and swallowed to allow processing of remaining entries.
     *
     * @param request the content entry to create
     * @return Mono that completes with the created entry, or empty if creation fails
     */
    private Mono<EntryCreateUpdate> upsertSingleEntry(MarketNewsEntry request) {
        log.debug("Creating content entry: title='{}', hasThumbnail={}", request.getTitle(),
            request.getThumbnailResource() != null);

        EntryCreateUpdateRequest createUpdateRequest = contentMapper.map(request);
        log.debug("Content entry request mapped: title='{}', request={}", request.getTitle(), createUpdateRequest);

        return Mono.defer(() -> Mono.just(contentApi.createContentEntry(createUpdateRequest)))
            .flatMap(entry -> addThumbnail(entry, request.getThumbnailResource()))
            .doOnSuccess(created -> log.info(
                "Content entry created successfully: title='{}', uuid={}, thumbnailAttached={}",
                request.getTitle(), created.getUuid(), request.getThumbnailResource() != null))
            .doOnError(error -> log.error(
                "Content entry creation failed: title='{}', errorType={}, errorMessage={}",
                request.getTitle(), error.getClass().getSimpleName(), error.getMessage(), error))
            .onErrorResume(error -> Mono.empty());
    }

    /**
     * Filters the supplied content entries to those not already present in the system.
     * An entry is considered a duplicate when its title contains an existing entry title.
     *
     * @param contentEntries entries to filter
     * @return Flux of entries that should be created
     */
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

                localVarFormParams.add("thumbnail", thumbnail);

                final String[] localVarAccepts = {"application/json"};
                final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
                final String[] localVarContentTypes = {"multipart/form-data"};
                final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

                String[] localVarAuthNames = new String[]{};

                ParameterizedTypeReference<EntryCreateUpdate> localReturnType = new ParameterizedTypeReference<>() {
                };
                apiClient.invokeAPI("/service-api/v2/content/entries/{uuid}/", HttpMethod.PATCH, uriVariables,
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
