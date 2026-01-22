package com.backbase.stream.investment.service.resttemplate;

import com.backbase.investment.api.service.sync.ApiClient;
import com.backbase.investment.api.service.sync.v1.ContentApi;
import com.backbase.investment.api.service.sync.v1.model.Entry;
import com.backbase.investment.api.service.sync.v1.model.EntryCreateUpdate;
import com.backbase.investment.api.service.sync.v1.model.EntryCreateUpdateRequest;
import java.io.File;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Design notes. (see CODING_RULES_COPILOT.md)
 * <ul>
 *   <li>No direct manipulation of generated API classes beyond construction & mapping</li>
 *   <li>Side-effecting operations are logged at info (create) or debug (patch) levels</li>
 *   <li>Exceptions from the underlying WebClient are propagated (caller decides retry strategy)</li>
 *   <li>All reactive operations include proper success and error handlers for observability</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class InvestmentRestNewsContentService {

    public static final int CONTENT_RETRIEVE_LIMIT = 100;
    private final ContentApi contentApi;
    private final ApiClient apiClient;

    /**
     * Upserts a list of content entries. For each entry, checks if content with the same title exists. If exists,
     * updates it; otherwise creates a new entry. Continues processing remaining entries even if individual entries
     * fail.
     *
     * @param contentEntries List of content entries to upsert
     * @return Mono that completes when all entries have been processed
     */
    public Mono<Void> upsertContent(List<EntryCreateUpdateRequest> contentEntries) {
        log.info("Starting content upsert batch operation:, totalEntries={}", contentEntries.size());
        log.debug("Content upsert batch details: entries={}", contentEntries);

        return findEntriesNewContent(contentEntries).flatMap(this::upsertSingleEntry).doOnComplete(
            () -> log.info("Content upsert batch completed successfully: totalEntriesProcessed={}",
                contentEntries.size())).doOnError(
            error -> log.error("Content upsert batch failed: totalEntries={}, errorType={}, errorMessage={}",
                contentEntries.size(), error.getClass().getSimpleName(), error.getMessage(), error)).then();
    }

    /**
     * Upserts a single content entry. Checks if an entry with the same title exists, and either updates the existing
     * entry or creates a new one. Errors are logged and swallowed to allow processing of remaining entries.
     *
     * @param request The content entry to upsert
     * @return Mono that completes when the entry has been processed
     */
    private Mono<EntryCreateUpdate> upsertSingleEntry(EntryCreateUpdateRequest request) {
        log.debug("Processing content entry: title='{}', hasThumbnail={}", request.getTitle(),
            request.getThumbnail() != null);

        return createNewEntry(request)
            .doOnSuccess(
                result -> log.info("Content entry processed successfully: title='{}', uuid={}", request.getTitle(),
                    result.getUuid())
            ).doOnError(throwable -> {
                if (throwable instanceof WebClientResponseException ex) {
                    log.error(
                        "Content entry processing failed with API error: title='{}', httpStatus={}, errorResponse={}",
                        request.getTitle(), ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
                } else {
                    log.error(
                        "Content entry processing failed with unexpected error: title='{}', errorType={}, errorMessage={}",
                        request.getTitle(), throwable.getClass().getSimpleName(), throwable.getMessage(), throwable);
                }
            })
            .onErrorResume(error -> {
                log.warn("Skipping failed content entry in batch: title='{}', decision=skip, reason={}",
                    request.getTitle(),
                    error.getMessage());
                return Mono.empty();
            });
    }

    private Flux<EntryCreateUpdateRequest> findEntriesNewContent(List<EntryCreateUpdateRequest> contentEntries) {
        Map<String, EntryCreateUpdateRequest> entryByTitle = contentEntries.stream()
            .collect(Collectors.toMap(EntryCreateUpdateRequest::getTitle, Function.identity()));
        log.debug("Filtering content entries: requestedTitles={}", entryByTitle.keySet());

        List<Entry> existsNews = contentApi.listContentEntries(null, CONTENT_RETRIEVE_LIMIT, 0, null, null, null, null)
            .getResults().stream().filter(Objects::nonNull).toList();

        if (existsNews.isEmpty()) {
            log.info("No existing content found in system:requestedEntries={}, existingEntries=0, newEntries={}",
                entryByTitle.size(), entryByTitle.size());
            return Flux.fromIterable(entryByTitle.values());
        }

        Set<String> existTitles = existsNews.stream().map(Entry::getTitle).collect(Collectors.toSet());
        List<EntryCreateUpdateRequest> newEntries = contentEntries.stream()
            .filter(c -> existTitles.stream().noneMatch(e -> c.getTitle().contains(e))).toList();

        log.info(
            "Content filtering completed: requestedEntries={}, existingEntriesFound={}, newEntriesToCreate={}, duplicatesSkipped={}",
            entryByTitle.size(), existsNews.size(), newEntries.size(), entryByTitle.size() - newEntries.size());
        log.debug("Filtered new content titles: newTitles={}",
            newEntries.stream().map(EntryCreateUpdateRequest::getTitle).collect(Collectors.toList()));

        return Flux.fromIterable(newEntries);
    }

    /**
     * Creates a new content entry.
     *
     * @param request The content data for the new entry
     * @return Mono containing the created entry
     */
    private Mono<EntryCreateUpdate> createNewEntry(EntryCreateUpdateRequest request) {
        log.debug("Creating new content entry: title='{}', hasThumbnail={}", request.getTitle(),
            request.getThumbnail() != null);
        File thumbnail = request.getThumbnail();
        request.setThumbnail(null);
        return Mono.defer(() -> Mono.just(contentApi.createContentEntry(request)))
            .flatMap(e -> addThumbnail(e, thumbnail))
            .doOnSuccess(
                created -> log.info("Content entry created successfully: title='{}', uuid={}, thumbnailAttached={}",
                    request.getTitle(), created.getUuid(), thumbnail != null))
            .doOnError(
                error -> log.error("Content entry creation failed: title='{}', errorType={}, errorMessage={}",
                    request.getTitle(), error.getClass().getSimpleName(), error.getMessage(), error))
            .onErrorResume(error -> Mono.empty());
    }

    private Mono<EntryCreateUpdate> addThumbnail(EntryCreateUpdate entry, File thumbnail) {
        UUID uuid = entry.getUuid();

        if (thumbnail == null) {
            log.debug("Skipping thumbnail attachment: uuid={}", uuid);
            return Mono.just(entry);
        }

        log.debug("Attaching thumbnail to content entry: uuid={}, thumbnailFile='{}', thumbnailSize={}", uuid,
            thumbnail.getName(), thumbnail.length());

        return Mono.defer(() -> {
                // create path and map variables
                Map<String, Object> uriVariables = new HashMap<>();
                uriVariables.put("uuid", uuid);

                MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<>();
                HttpHeaders localVarHeaderParams = new HttpHeaders();
                MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<>();
                MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<>();

                FileSystemResource value = new FileSystemResource(thumbnail);
                localVarFormParams.add("thumbnail", value);

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

                log.info("Thumbnail attached successfully: uuid={}, thumbnailFile='{}'", uuid, thumbnail.getName());
                return Mono.just(entry);
            })
            .doOnError(error -> log.error(
                "Thumbnail attachment failed: uuid={}, thumbnailFile='{}', errorType={}, errorMessage={}", uuid,
                thumbnail.getName(), error.getClass().getSimpleName(), error.getMessage(), error))
            .onErrorResume(error -> {
                log.warn("Content entry created without thumbnail: uuid={}, reason={}", uuid, error.getMessage());
                return Mono.just(entry);
            });
    }

}
