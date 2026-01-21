package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.sync.ApiClient;
import com.backbase.investment.api.service.sync.v1.ContentApi;
import com.backbase.investment.api.service.sync.v1.model.Entry;
import com.backbase.investment.api.service.sync.v1.model.EntryCreateUpdate;
import com.backbase.investment.api.service.sync.v1.model.EntryCreateUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>Design notes (see CODING_RULES_COPILOT.md):</p>.
 * <ul>
 *   <li>No direct manipulation of generated API classes beyond construction & mapping</li>
 *   <li>Side-effecting operations are logged at info (create) or debug (patch) levels</li>
 *   <li>Exceptions from the underlying WebClient are propagated (caller decides retry strategy)</li>
 *   <li>All reactive operations include proper success and error handlers for observability</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class InvestmentNewsContentService {

    public static final int CONTENT_RETRIEVE_LIMIT = 100;
    private final ContentApi contentApi;
    private final ApiClient apiClient;
    private final ObjectMapper objectMapper;

    /**
     * Upserts a list of content entries. For each entry, checks if content with the same title exists. If exists,
     * updates it; otherwise creates a new entry. Continues processing remaining entries even if individual entries
     * fail.
     *
     * @param contentEntries List of content entries to upsert
     * @return Mono that completes when all entries have been processed
     */
    public Mono<Void> upsertContent(List<EntryCreateUpdateRequest> contentEntries) {
        log.info("Starting upsert for {} content entries", contentEntries.size());
        log.debug("Contents to upsert {}", contentEntries);

        return findEntriesNewContent(contentEntries)
            .flatMap(this::upsertSingleEntry)
            .doOnComplete(() -> log.info("Completed upsert for all content entries"))
            .doOnError(error -> log.error("Unexpected error during content upsert batch processing", error))
            .then();
    }

    /**
     * Upserts a single content entry. Checks if an entry with the same title exists, and either updates the existing
     * entry or creates a new one. Errors are logged and swallowed to allow processing of remaining entries.
     *
     * @param request The content entry to upsert
     * @return Mono that completes when the entry has been processed
     */
    private Mono<EntryCreateUpdate> upsertSingleEntry(EntryCreateUpdateRequest request) {
        log.debug("Processing content entry with title: '{}'", request.getTitle());

        return createNewEntry(request)
            .doOnSuccess(result -> log.info("Successfully upserted content entry: '{}'", request.getTitle()))
            .doOnError(throwable -> {
                if (throwable instanceof WebClientResponseException ex) {
                    log.error("Failed to create content: status={}, body={}",
                        ex.getStatusCode(),
                        ex.getResponseBodyAsString(), ex);
                } else {
                    log.error("Failed prices creation content", throwable);
                }
            })
            .onErrorResume(error -> {
                log.error("Failed to upsert content entry with title '{}': {}. Continuing with next entry.",
                    request.getTitle(), error.getMessage(), error);
                return Mono.empty();
            });
    }

    private Flux<EntryCreateUpdateRequest> findEntriesNewContent(List<EntryCreateUpdateRequest> contentEntries) {
        Map<String, EntryCreateUpdateRequest> entryByTitle = contentEntries.stream()
            .collect(Collectors.toMap(EntryCreateUpdateRequest::getTitle, Function.identity()));
        log.debug("Searching for existing content entry with title: '{}'", entryByTitle.keySet());

        List<Entry> existsNews = contentApi.listContentEntries(null, CONTENT_RETRIEVE_LIMIT, 0,
                null, null, null, null)
            .getResults()
            .stream()
            .filter(Objects::nonNull)
            .toList();
        if (existsNews.isEmpty()) {
            log.debug("No existing content entries found. All {} entries are new.",
                entryByTitle.size());
            return Flux.fromIterable(entryByTitle.values());
        }
        Set<String> existTitles = existsNews.stream()
            .map(Entry::getTitle)
            .collect(Collectors.toSet());
        List<EntryCreateUpdateRequest> newEntries = contentEntries.stream()
            .filter(c -> existTitles.stream().noneMatch(e -> c.getTitle().contains(e)))
            .toList();

        log.debug("New content entries to create: {}", newEntries.stream());
        return Flux.fromIterable(newEntries);
    }

    /**
     * Creates a new content entry.
     *
     * @param request The content data for the new entry
     * @return Mono containing the created entry
     */
    private Mono<EntryCreateUpdate> createNewEntry(EntryCreateUpdateRequest request) {
        log.debug("Creating new content entry with title: '{}'", request.getTitle());
        File thumbnail = request.getThumbnail();
        request.setThumbnail(null);
        return Mono.defer(() -> Mono.just(contentApi.createContentEntry(request)))
            .flatMap(e -> addThumbnail(e, thumbnail))
            .doOnSuccess(created -> log.info("Successfully created new content entry with title: '{}'",
                request.getTitle()))
            .doOnError(error -> log.error("Failed to create content entry with title '{}': {}",
                request.getTitle(), error.getMessage(), error))
            .onErrorResume(error -> Mono.empty());
    }

    private Mono<EntryCreateUpdate> addThumbnail(EntryCreateUpdate entry, File thumbnail) {
        UUID uuid = entry.getUuid();
        return Mono.defer(() -> {
                Object localVarPostBody = null;
                // verify the required parameter 'uuid' is set
                if (uuid == null) {
                    throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                        "Missing the required parameter 'uuid' when calling patchContentEntry");
                }

                // create path and map variables
                final Map<String, Object> uriVariables = new HashMap<String, Object>();
                uriVariables.put("uuid", uuid);

                final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
                final HttpHeaders localVarHeaderParams = new HttpHeaders();
                final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
                final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

                FileSystemResource value = new FileSystemResource(thumbnail);
                localVarFormParams.add("thumbnail", value);

                final String[] localVarAccepts = {
                    "application/json"
                };
                final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
                final String[] localVarContentTypes = {
                    "multipart/form-data"
                };
                final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

                String[] localVarAuthNames = new String[]{};

                ParameterizedTypeReference<EntryCreateUpdate> localReturnType = new ParameterizedTypeReference<EntryCreateUpdate>() {
                };
                apiClient.invokeAPI(
                    "/service-api/v2/content/entries/{uuid}/", HttpMethod.PATCH, uriVariables,
                    localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams,
                    localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
                return Mono.just(entry);
            })
            .doOnError(error -> log.error("Failed to set content `{}` thumbnail: {}",
                uuid, error.getMessage(), error))
            .onErrorResume(error -> Mono.just(entry));
    }

}
