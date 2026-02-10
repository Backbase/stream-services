package com.backbase.stream.investment.service.resttemplate;


import com.backbase.investment.api.service.sync.ApiClient;
import com.backbase.investment.api.service.sync.v1.ContentApi;
import com.backbase.investment.api.service.sync.v1.model.DocumentTagRequest;
import com.backbase.investment.api.service.sync.v1.model.OASDocumentRequestDataRequest;
import com.backbase.investment.api.service.sync.v1.model.OASDocumentResponse;
import com.backbase.investment.api.service.sync.v1.model.PatchedDocumentTagRequest;
import com.backbase.stream.investment.model.ContentDocumentEntry;
import com.backbase.stream.investment.model.ContentTag;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

@Slf4j
@RequiredArgsConstructor
public class InvestmentRestDocumentContentService {

    private static final Integer CONTENT_RETRIEVE_LIMIT = 100;
    private final ContentApi contentApi;
    private final ApiClient apiClient;
    private final ContentMapper contentMapper = Mappers.getMapper(ContentMapper.class);

    public Mono<Void> upsertContentTags(List<ContentTag> documentTags) {
        log.info("Starting document tag upsert batch operation: totalEntries={}", documentTags.size());
        log.debug("Tag document upsert batch details: entries={}", documentTags);

        return Flux.fromIterable(documentTags)
            .flatMap(this::upsertSingleTag)
            .doOnComplete(() -> log.info("Document Tag upsert batch completed successfully: totalEntriesProcessed={}",
                documentTags.size()))
            .doOnError(
                error -> log.error("Document Tag upsert batch failed: totalEntries={}, errorType={}, errorMessage={}",
                    documentTags.size(), error.getClass().getSimpleName(), error.getMessage(), error))
            .then();
    }

    private Mono<ContentTag> upsertSingleTag(ContentTag documentTag) {
        log.debug("Processing Document tag: code='{}', value='{}'", documentTag.getCode(), documentTag.getValue());

        // Validation
        if (documentTag.getCode() == null || documentTag.getCode().isBlank()) {
            log.warn("Skipping Document tag with empty code: value='{}'", documentTag.getValue());
            return Mono.empty();
        }

        if (documentTag.getValue() == null || documentTag.getValue().isBlank()) {
            log.warn("Skipping Document tag with empty value: code='{}'", documentTag.getCode());
            return Mono.empty();
        }

        log.debug("Checking if Document tag entry exists: code='{}', value='{}'",
            documentTag.getCode(), documentTag.getValue());

        // Check if tag entry already exists
        return Mono.fromCallable(() ->
                contentApi.contentDocumentTagList(CONTENT_RETRIEVE_LIMIT, 0))
            .map(paginatedList -> paginatedList.getResults().stream()
                .filter(Objects::nonNull)
                .filter(entry -> documentTag.getCode().equals(entry.getCode()))
                .findFirst())
            .flatMap(existingEntry -> {
                if (existingEntry.isPresent()) {
                    log.info("Document Tag entry already exists: code='{}', value='{}'",
                        documentTag.getCode(), documentTag.getValue());
                    return patchTagEntry(documentTag);
                } else {
                    // Create new tag entry
                    log.debug("Creating new Document tag entry: code='{}', value='{}'",
                        documentTag.getCode(), documentTag.getValue());
                    return createTagEntry(documentTag);
                }
            })
            .doOnSuccess(tag -> log.info("Document Tag upsert completed successfully: code='{}', value='{}'",
                tag.getCode(), tag.getValue()))
            .doOnError(error -> log.error(
                "Document Tag upsert failed: code='{}', value='{}', errorType={}, errorMessage={}",
                documentTag.getCode(), documentTag.getValue(),
                error.getClass().getSimpleName(), error.getMessage(), error))
            .onErrorResume(error -> {
                log.warn("Continuing without Document tag: code='{}', reason={}",
                    documentTag.getCode(), error.getMessage());
                return Mono.empty();
            });
    }

    private Mono<ContentTag> patchTagEntry(ContentTag contentTag) {
        PatchedDocumentTagRequest request = new PatchedDocumentTagRequest()
            .code(contentTag.getCode())
            .value(contentTag.getValue());

        return Mono.defer(() -> Mono.just(contentApi.contentDocumentTagPartialUpdate(contentTag.getCode(), request)))
            .doOnSuccess(patched -> log.info(
                "Document Tag entry patched successfully: code='{}', value='{}'",
                patched.getCode(), patched.getValue()))
            .doOnError(error -> log.error(
                "Document Tag entry patch failed: code='{}', value='{}', errorType={}, errorMessage={}",
                contentTag.getCode(), contentTag.getValue(),
                error.getClass().getSimpleName(), error.getMessage(), error))
            .thenReturn(contentTag);
    }

    private Mono<ContentTag> createTagEntry(ContentTag contentTag) {
        DocumentTagRequest request = new DocumentTagRequest()
            .code(contentTag.getCode())
            .value(contentTag.getValue());

        return Mono.defer(() -> Mono.just(contentApi.contentDocumentTagCreate(request)))
            .doOnSuccess(created -> log.info(
                "Document Tag entry created successfully: code='{}', value='{}'",
                created.getCode(), created.getValue()))
            .doOnError(error -> log.error(
                "Document Tag entry creation failed: code='{}', value='{}', errorType={}, errorMessage={}",
                contentTag.getCode(), contentTag.getValue(),
                error.getClass().getSimpleName(), error.getMessage(), error))
            .thenReturn(contentTag);
    }

    public Mono<Void> upsertDocuments(List<ContentDocumentEntry> documents) {
        log.info("Starting document upsert batch operation: totalEntries={}", documents.size());
        log.debug("Document upsert batch details: entries={}", documents);

        return findEntriesNewContent(documents)
            .flatMap(this::insertDocument)
            .doOnComplete(
                () -> log.info("Document upsert batch completed successfully: totalEntriesProcessed={}",
                    documents.size()))
            .doOnError(
                error -> log.error("Document upsert batch failed: totalEntries={}, errorType={}, errorMessage={}",
                    documents.size(), error.getClass().getSimpleName(), error.getMessage(), error))
            .then();
    }

    private Mono<ContentDocumentEntry> insertDocument(ContentDocumentEntry request) {
        log.debug("Processing document entry: title='{}', hasDocument={}", request.getName(),
            request.getResourceInPath() != null);

        OASDocumentRequestDataRequest createDocumentRequest = contentMapper.map(request);
        log.debug("Document entry request mapped: {}", createDocumentRequest);
        return Mono.defer(() -> Mono.just(createContentDocument(createDocumentRequest, request.getResourceInPath()))
            .doOnSuccess(
                created -> log.info("Document entry created successfully: title='{}', uuid={}, documentAttached={}",
                    request.getName(), created.getUuid(), request.getResourceInPath() != null))
            .doOnError(
                error -> log.error("Document entry creation failed: title='{}', errorType={}, errorMessage={}",
                    request.getName(), error.getClass().getSimpleName(), error.getMessage(), error))
            .onErrorResume(error -> Mono.empty())
            .thenReturn(request));
    }

    private Flux<ContentDocumentEntry> findEntriesNewContent(List<ContentDocumentEntry> documents) {
        Map<String, ContentDocumentEntry> entryByTitle = documents.stream()
            .collect(Collectors.toMap(ContentDocumentEntry::getName, Function.identity()));
        log.debug("Filtering document entries: requestedTitles={}", entryByTitle.keySet());

        List<OASDocumentResponse> existsNews = contentApi.listContentDocuments(null, CONTENT_RETRIEVE_LIMIT, null, 0,
                null, null)
            .getResults().stream().filter(Objects::nonNull).toList();

        if (existsNews.isEmpty()) {
            log.info("No existing document found in system: requestedEntries={}, existingEntries=0, newEntries={}",
                entryByTitle.size(), entryByTitle.size());
            return Flux.fromIterable(entryByTitle.values());
        }

        Set<String> existTitles = existsNews.stream().map(OASDocumentResponse::getName).collect(Collectors.toSet());
        List<ContentDocumentEntry> newEntries = documents.stream()
            .filter(c -> existTitles.stream().noneMatch(e -> c.getName().equals(e))).toList();

        log.info(
            "Document filtering completed: requestedEntries={}, existingEntriesFound={}, "
                + "newEntriesToCreate={}, duplicatesSkipped={}",
            entryByTitle.size(), existsNews.size(), newEntries.size(), entryByTitle.size() - newEntries.size());
        log.debug("Filtered new Document names: newTitles={}",
            newEntries.stream().map(ContentDocumentEntry::getName).collect(Collectors.toList()));

        return Flux.fromIterable(newEntries);
    }

    public OASDocumentResponse createContentDocument(OASDocumentRequestDataRequest data, Resource document)
        throws RestClientException {

        HttpHeaders localVarHeaderParams = new HttpHeaders();
        MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<>();
        MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<>();

        if (data != null) {
            localVarFormParams.add("data", data);
        }
        if (document != null) {
            localVarFormParams.add("path", document);
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

        ParameterizedTypeReference<OASDocumentResponse> localReturnType = new ParameterizedTypeReference<>() {
        };
        return apiClient.invokeAPI("/service-api/v2/content/documents/", HttpMethod.POST,
                Collections.<String, Object>emptyMap(), new LinkedMultiValueMap<>(), null, localVarHeaderParams,
                localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames,
                localReturnType)
            .getBody();
    }

}
