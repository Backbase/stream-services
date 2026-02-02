package com.backbase.stream.investment.service.resttemplate;

import static com.backbase.stream.investment.service.resttemplate.InvestmentRestAssetUniverseService.getFileNameForLog;

import com.backbase.investment.api.service.sync.ApiClient;
import com.backbase.investment.api.service.sync.v1.ContentApi;
import com.backbase.investment.api.service.sync.v1.model.Entry;
import com.backbase.investment.api.service.sync.v1.model.EntryCreateUpdate;
import com.backbase.investment.api.service.sync.v1.model.EntryCreateUpdateRequest;
import com.backbase.stream.investment.model.MarketNewsEntry;
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
    private final ContentMapper contentMapper = Mappers.getMapper(ContentMapper.class);

    /**
     * Upserts a list of content entries. For each entry, checks if content with the same title exists. If exists,
     * updates it; otherwise creates a new entry. Continues processing remaining entries even if individual entries
     * fail.
     *
     * @param contentEntries List of content entries to upsert
     * @return Mono that completes when all entries have been processed
     */
    public Mono<Void> upsertContent(List<MarketNewsEntry> contentEntries) {
        log.info("Starting content upsert batch operation:, totalEntries={}", contentEntries.size());
        log.debug("Content upsert batch details: entries={}", contentEntries);

        return findEntriesNewContent(contentEntries).flatMap(this::upsertSingleEntry).doOnComplete(
                () -> log.info("Content upsert batch completed successfully: totalEntriesProcessed={}",
                    contentEntries.size()))
            .doOnError(error -> log.error("Content upsert batch failed: totalEntries={}, errorType={}, errorMessage={}",
                contentEntries.size(), error.getClass().getSimpleName(), error.getMessage(), error)).then();
    }

    /**
     * Upserts a single content entry. Checks if an entry with the same title exists, and either updates the existing
     * entry or creates a new one. Errors are logged and swallowed to allow processing of remaining entries.
     *
     * @param request The content entry to upsert
     * @return Mono that completes when the entry has been processed
     */
    private Mono<EntryCreateUpdate> upsertSingleEntry(MarketNewsEntry request) {
        log.debug("Processing content entry: title='{}', hasThumbnail={}", request.getTitle(),
            request.getThumbnailResource() != null);

        log.debug("Creating new content entry: title='{}', hasThumbnail={}", request.getTitle(),
            request.getThumbnailResource() != null);
        EntryCreateUpdateRequest createUpdateRequest = contentMapper.map(request);
        log.debug("Content entry processing : {}", createUpdateRequest);
        return Mono.defer(() -> Mono.just(contentApi.createContentEntry(createUpdateRequest)))
            .flatMap(e -> addThumbnail(e, request.getThumbnailResource()))
            .doOnSuccess(
                created -> log.info("Content entry created successfully: title='{}', uuid={}, thumbnailAttached={}",
                    request.getTitle(), created.getUuid(), request.getThumbnailResource() != null))
            .doOnError(
                error -> log.error("Content entry creation failed: title='{}', errorType={}, errorMessage={}",
                    request.getTitle(), error.getClass().getSimpleName(), error.getMessage(), error))
            .onErrorResume(error -> Mono.empty());
    }

    private Flux<MarketNewsEntry> findEntriesNewContent(List<MarketNewsEntry> contentEntries) {
        Map<String, MarketNewsEntry> entryByTitle = contentEntries.stream()
            .collect(Collectors.toMap(MarketNewsEntry::getTitle, Function.identity()));
        log.debug("Filtering content entries: requestedTitles={}", entryByTitle.keySet());

        List<Entry> existsNews = contentApi.listContentEntries(null, CONTENT_RETRIEVE_LIMIT, 0, null, null, null, null)
            .getResults().stream().filter(Objects::nonNull).toList();

        if (existsNews.isEmpty()) {
            log.info("No existing content found in system:requestedEntries={}, existingEntries=0, newEntries={}",
                entryByTitle.size(), entryByTitle.size());
            return Flux.fromIterable(entryByTitle.values());
        }

        Set<String> existTitles = existsNews.stream().map(Entry::getTitle).collect(Collectors.toSet());
        List<MarketNewsEntry> newEntries = contentEntries.stream()
            .filter(c -> existTitles.stream().noneMatch(e -> c.getTitle().contains(e))).toList();

        log.info(
            "Content filtering completed: requestedEntries={}, existingEntriesFound={}, newEntriesToCreate={}, duplicatesSkipped={}",
            entryByTitle.size(), existsNews.size(), newEntries.size(), entryByTitle.size() - newEntries.size());
        log.debug("Filtered new content titles: newTitles={}",
            newEntries.stream().map(MarketNewsEntry::getTitle).collect(Collectors.toList()));

        return Flux.fromIterable(newEntries);
    }

    private Mono<EntryCreateUpdate> addThumbnail(EntryCreateUpdate entry, Resource thumbnail) {
        UUID uuid = entry.getUuid();

        if (thumbnail == null) {
            log.debug("Skipping thumbnail attachment: uuid={}", uuid);
            return Mono.just(entry);
        }

        log.debug("Attaching thumbnail to content entry: uuid={}, thumbnailFile='{}'", uuid,
            getFileNameForLog(thumbnail));

        return Mono.defer(() -> {
            // create path and map variables
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

            log.info("Thumbnail attached successfully: uuid={}, thumbnailFile='{}'", uuid,
                getFileNameForLog(thumbnail));
            return Mono.just(entry);
        }).doOnError(error -> log.error(
            "Thumbnail attachment failed: uuid={}, thumbnailFile='{}', errorType={}, errorMessage={}", uuid,
            getFileNameForLog(thumbnail), error.getClass().getSimpleName(), error.getMessage(), error))
        .onErrorResume(error -> {
            log.warn("Content entry created without thumbnail: uuid={}, reason={}", uuid, error.getMessage());
            return Mono.just(entry);
        });
    }

}
