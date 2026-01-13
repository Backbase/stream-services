package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.ApiClient;
import com.backbase.investment.api.service.v1.ContentApi;
import com.backbase.investment.api.service.v1.model.EntryCreateUpdate;
import com.backbase.investment.api.service.v1.model.EntryCreateUpdateRequest;
import com.backbase.investment.api.service.v1.model.PaginatedEntryList;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>Design notes (see CODING_RULES_COPILOT.md):
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

        return contentApi.listContentEntries(null, CONTENT_RETRIEVE_LIMIT, 0,
                null, null, null, null)
            .doOnSuccess(result -> log.debug("Retrieved {} content entries for title search",
                Optional.ofNullable(result).map(PaginatedEntryList::getCount).orElse(0)))
            .map(PaginatedEntryList::getResults)
            .filter(Objects::nonNull)
            .filter(List::isEmpty)
            .doOnError(error -> log.error("Error searching for content entry with titles '{}': {}",
                entryByTitle.keySet(), error.getMessage(), error))
            .flatMapIterable(entries -> {
                List<EntryCreateUpdateRequest> newEntries = entries.isEmpty() ? contentEntries : entries.stream()
                    .filter(e -> entryByTitle.containsKey(e.getTitle()))
                    .map(e -> entryByTitle.get(e.getTitle())).toList();
                log.debug("New content entries to create: {}", newEntries.stream()
                    .map(EntryCreateUpdateRequest::getTitle).collect(Collectors.toList()));
                return newEntries;
            });
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
        return contentApi.createContentEntry(request)

            .doOnSuccess(created -> log.info("Successfully created new content entry with title: '{}'",
                request.getTitle()))
            .doOnError(error -> log.error("Failed to create content entry with title '{}': {}",
                request.getTitle(), error.getMessage(), error));
    }

    private Mono<EntryCreateUpdate> createContentEntry(EntryCreateUpdateRequest entryCreateUpdateRequest)
        throws WebClientResponseException {
        Object postBody = entryCreateUpdateRequest;
        // verify the required parameter 'entryCreateUpdateRequest' is set
        if (entryCreateUpdateRequest == null) {
            throw new WebClientResponseException(
                "Missing the required parameter 'entryCreateUpdateRequest' when calling createContentEntry",
                HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        FileSystemResource value = new FileSystemResource(entryCreateUpdateRequest.getThumbnail());

        entryCreateUpdateRequest.setThumbnail(null);

        final String[] localVarAccepts = {
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {
            "application/json"
        };
        final MediaType localVarContentType = MediaType.MULTIPART_FORM_DATA;

        String[] localVarAuthNames = new String[]{};

        ParameterizedTypeReference<EntryCreateUpdate> localVarReturnType = new ParameterizedTypeReference<EntryCreateUpdate>() {
        };
        return apiClient.invokeAPI("/service-api/v2/content/entries/", HttpMethod.POST, pathParams, queryParams,
                postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames,
                localVarReturnType)
            .bodyToMono(localVarReturnType)
            .flatMap(e -> patchContentEntryRequestCreation(e.getUuid(), value));
    }

    private Mono<EntryCreateUpdate> patchContentEntryRequestCreation(
        UUID uuid, FileSystemResource value) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'uuid' is set
        if (uuid == null) {
            throw new WebClientResponseException("Missing the required parameter 'uuid' when calling patchContentEntry",
                HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("uuid", uuid);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        formParams.add("thumbnail", value);

        final String[] localVarAccepts = {
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {
            "multipart/form-data"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};

        ParameterizedTypeReference<EntryCreateUpdate> localVarReturnType = new ParameterizedTypeReference<EntryCreateUpdate>() {
        };
        return apiClient.invokeAPI("/service-api/v2/content/entries/{uuid}/", HttpMethod.PATCH, pathParams, queryParams,
                postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames,
                localVarReturnType)
            .bodyToMono(localVarReturnType);
    }

}
