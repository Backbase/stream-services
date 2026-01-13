package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.ApiClient;
import com.backbase.investment.api.service.v1.ContentApi;
import com.backbase.investment.api.service.v1.model.Entry;
import com.backbase.investment.api.service.v1.model.EntryCreateUpdate;
import com.backbase.investment.api.service.v1.model.EntryCreateUpdateRequest;
import com.backbase.investment.api.service.v1.model.PaginatedEntryList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InvestmentNewsContentServiceTest {

    private ContentApi contentApi;
    private ApiClient apiClient;
    private InvestmentNewsContentService service;

    @BeforeEach
    void setUp() {
        contentApi = Mockito.mock(ContentApi.class);
        apiClient = Mockito.mock(ApiClient.class);
        service = new InvestmentNewsContentService(contentApi, apiClient);
    }

    @Test
    void upsertContent_createsNewEntry_whenNotExists() {
        // Given
        EntryCreateUpdateRequest request = new EntryCreateUpdateRequest()
                .title("New Article")
                .excerpt("Excerpt")
                .tags(List.of("tag1"));

        PaginatedEntryList emptyList = new PaginatedEntryList()
                .count(0)
                .results(List.of());

        EntryCreateUpdate created = new EntryCreateUpdate();

        when(contentApi.listContentEntries(isNull(), eq(100), eq(0), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(emptyList));
        when(contentApi.createContentEntry(any(EntryCreateUpdateRequest.class)))
                .thenReturn(Mono.just(created));

        // When & Then
        StepVerifier.create(service.upsertContent(List.of(request)))
                .verifyComplete();

        verify(contentApi).listContentEntries(isNull(), eq(100), eq(0), isNull(), isNull(), isNull(), isNull());
        verify(contentApi).createContentEntry(request);
        verify(contentApi, never()).updateContentEntry(anyString(), any());
    }

    @Test
    void upsertContent_updatesExistingEntry_whenExists() {
        // Given
        String existingTitle = "Existing Article";
        UUID existingUuid = UUID.randomUUID();

        EntryCreateUpdateRequest request = new EntryCreateUpdateRequest()
                .title(existingTitle)
                .excerpt("Updated excerpt")
                .tags(List.of("tag1"));

        Entry existingEntry = new Entry(
                existingUuid,
                existingTitle,
                "Old excerpt",
                "Body",
                null,
                null,
                null,
                null,
                null,
                List.of()
        );

        PaginatedEntryList listWithEntry = new PaginatedEntryList()
                .count(1)
                .results(List.of(existingEntry));

        EntryCreateUpdate updated = new EntryCreateUpdate();

        when(contentApi.listContentEntries(isNull(), eq(100), eq(0), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(listWithEntry));
        when(contentApi.updateContentEntry(eq(existingUuid.toString()), any(EntryCreateUpdateRequest.class)))
                .thenReturn(Mono.just(updated));

        // When & Then
        StepVerifier.create(service.upsertContent(List.of(request)))
                .verifyComplete();

        verify(contentApi).listContentEntries(isNull(), eq(100), eq(0), isNull(), isNull(), isNull(), isNull());
        verify(contentApi).updateContentEntry(existingUuid.toString(), request);
        verify(contentApi, never()).createContentEntry(any());
    }

    @Test
    void upsertContent_continuesProcessing_whenOneEntryFails() {
        // Given
        EntryCreateUpdateRequest request1 = new EntryCreateUpdateRequest()
                .title("Article 1")
                .tags(List.of("tag1"));

        EntryCreateUpdateRequest request2 = new EntryCreateUpdateRequest()
                .title("Article 2")
                .tags(List.of("tag2"));

        PaginatedEntryList emptyList = new PaginatedEntryList()
                .count(0)
                .results(List.of());

        EntryCreateUpdate created = new EntryCreateUpdate();

        WebClientResponseException error = new WebClientResponseException(
                500, "Internal Server Error", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);

        when(contentApi.listContentEntries(isNull(), eq(100), eq(0), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(emptyList));
        when(contentApi.createContentEntry(request1))
                .thenReturn(Mono.error(error));
        when(contentApi.createContentEntry(request2))
                .thenReturn(Mono.just(created));

        // When & Then
        StepVerifier.create(service.upsertContent(Arrays.asList(request1, request2)))
                .verifyComplete();

        verify(contentApi, times(2)).listContentEntries(isNull(), eq(100), eq(0), isNull(), isNull(), isNull(), isNull());
        verify(contentApi).createContentEntry(request1);
        verify(contentApi).createContentEntry(request2);
    }

    @Test
    void upsertContent_handlesMultipleEntries() {
        // Given
        UUID existingUuid = UUID.randomUUID();
        String existingTitle = "Existing Article";

        EntryCreateUpdateRequest request1 = new EntryCreateUpdateRequest()
                .title(existingTitle)
                .tags(List.of("tag1"));

        EntryCreateUpdateRequest request2 = new EntryCreateUpdateRequest()
                .title("New Article")
                .tags(List.of("tag2"));

        Entry existingEntry = new Entry(
                existingUuid,
                existingTitle,
                "Excerpt",
                "Body",
                null,
                null,
                null,
                null,
                null,
                List.of()
        );

        PaginatedEntryList listWithEntry = new PaginatedEntryList()
                .count(1)
                .results(List.of(existingEntry));

        PaginatedEntryList emptyList = new PaginatedEntryList()
                .count(0)
                .results(List.of());

        EntryCreateUpdate result = new EntryCreateUpdate();

        when(contentApi.listContentEntries(isNull(), eq(100), eq(0), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(listWithEntry))
                .thenReturn(Mono.just(emptyList));
        when(contentApi.updateContentEntry(eq(existingUuid.toString()), any()))
                .thenReturn(Mono.just(result));
        when(contentApi.createContentEntry(any()))
                .thenReturn(Mono.just(result));

        // When & Then
        StepVerifier.create(service.upsertContent(Arrays.asList(request1, request2)))
                .verifyComplete();

        verify(contentApi, times(2)).listContentEntries(isNull(), eq(100), eq(0), isNull(), isNull(), isNull(), isNull());
        verify(contentApi).updateContentEntry(existingUuid.toString(), request1);
        verify(contentApi).createContentEntry(request2);
    }

    @Test
    void upsertContent_handlesEmptyList() {
        // When & Then
        StepVerifier.create(service.upsertContent(List.of()))
                .verifyComplete();

        verifyNoInteractions(contentApi);
    }

    @Test
    void upsertContent_handlesSearchError_continuesProcessing() {
        // Given
        EntryCreateUpdateRequest request = new EntryCreateUpdateRequest()
                .title("Article")
                .tags(List.of("tag1"));

        WebClientResponseException searchError = new WebClientResponseException(
                503, "Service Unavailable", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);

        when(contentApi.listContentEntries(isNull(), eq(100), eq(0), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.error(searchError));

        // When & Then
        StepVerifier.create(service.upsertContent(List.of(request)))
                .verifyComplete();

        verify(contentApi).listContentEntries(isNull(), eq(100), eq(0), isNull(), isNull(), isNull(), isNull());
        verify(contentApi, never()).createContentEntry(any());
        verify(contentApi, never()).updateContentEntry(anyString(), any());
    }

    @Test
    void upsertContent_filtersCorrectlyByTitle() {
        // Given
        String targetTitle = "Target Article";
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        EntryCreateUpdateRequest request = new EntryCreateUpdateRequest()
                .title(targetTitle)
                .tags(List.of("tag1"));

        Entry entry1 = new Entry(
                uuid1,
                "Different Article",
                "Excerpt 1",
                "Body 1",
                null,
                null,
                null,
                null,
                null,
                List.of()
        );

        Entry entry2 = new Entry(
                uuid2,
                targetTitle,
                "Excerpt 2",
                "Body 2",
                null,
                null,
                null,
                null,
                null,
                List.of()
        );

        PaginatedEntryList list = new PaginatedEntryList()
                .count(2)
                .results(Arrays.asList(entry1, entry2));

        EntryCreateUpdate updated = new EntryCreateUpdate();

        when(contentApi.listContentEntries(isNull(), eq(100), eq(0), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(list));
        when(contentApi.updateContentEntry(eq(uuid2.toString()), any()))
                .thenReturn(Mono.just(updated));

        // When & Then
        StepVerifier.create(service.upsertContent(List.of(request)))
                .verifyComplete();

        verify(contentApi).updateContentEntry(uuid2.toString(), request);
        verify(contentApi, never()).createContentEntry(any());
    }
}

