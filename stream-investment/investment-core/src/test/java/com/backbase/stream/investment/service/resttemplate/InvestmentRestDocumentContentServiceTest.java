package com.backbase.stream.investment.service.resttemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.sync.ApiClient;
import com.backbase.investment.api.service.sync.v1.ContentApi;
import com.backbase.investment.api.service.sync.v1.model.DocumentTag;
import com.backbase.investment.api.service.sync.v1.model.OASDocumentResponse;
import com.backbase.investment.api.service.sync.v1.model.PaginatedDocumentTagList;
import com.backbase.investment.api.service.sync.v1.model.PaginatedOASDocumentResponseList;
import com.backbase.stream.investment.model.ContentDocumentEntry;
import com.backbase.stream.investment.model.ContentTag;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class InvestmentRestDocumentContentServiceTest {

    @Mock
    private ContentApi contentApi;

    @Mock
    private ApiClient apiClient;

    private InvestmentRestDocumentContentService service;

    @BeforeEach
    void setUp() {
        service = new InvestmentRestDocumentContentService(contentApi, apiClient);
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> invalidTags() {
        return Stream.of(
            org.junit.jupiter.params.provider.Arguments.of("   ", "value1"),   // blank code
            org.junit.jupiter.params.provider.Arguments.of("code1", null),     // null value
            org.junit.jupiter.params.provider.Arguments.of("code1", "   ")     // blank value
        );
    }

    // -----------------------------------------------------------------------
    // upsertContentTags
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("upsertContentTags")
    class UpsertContentTags {

        @Test
        @DisplayName("empty list completes without calling API")
        void emptyListCompletesWithoutApiCall() {
            StepVerifier.create(service.upsertContentTags(List.of()))
                .verifyComplete();

            verify(contentApi, never()).contentDocumentTagList(anyInt(), anyInt());
        }

        @Test
        @DisplayName("tag with null code is skipped")
        void tagWithNullCodeIsSkipped() {
            ContentTag tag = new ContentTag(null, "someValue");

            StepVerifier.create(service.upsertContentTags(List.of(tag)))
                .verifyComplete();

            verify(contentApi, never()).contentDocumentTagList(anyInt(), anyInt());
            verify(contentApi, never()).contentDocumentTagCreate(any());
            verify(contentApi, never()).contentDocumentTagPartialUpdate(any(), any());
        }

        @ParameterizedTest(name = "tag skipped when code=''{0}'' value=''{1}''")
        @DisplayName("tag with blank/null code or blank/null value is skipped")
        @MethodSource("com.backbase.stream.investment.service.resttemplate.InvestmentRestDocumentContentServiceTest#invalidTags")
        void tagWithInvalidCodeOrValueIsSkipped(String code, String value) {
            ContentTag tag = new ContentTag(code, value);

            StepVerifier.create(service.upsertContentTags(List.of(tag)))
                .verifyComplete();

            verify(contentApi, never()).contentDocumentTagList(anyInt(), anyInt());
            verify(contentApi, never()).contentDocumentTagCreate(any());
            verify(contentApi, never()).contentDocumentTagPartialUpdate(any(), any());
        }

        @Test
        @DisplayName("new tag (not existing) triggers create")
        void newTagTriggersCreate() {
            ContentTag tag = new ContentTag("docCode1", "docValue1");
            PaginatedDocumentTagList emptyPage = new PaginatedDocumentTagList().results(List.of());
            DocumentTag created = new DocumentTag().code("docCode1").value("docValue1");

            when(contentApi.contentDocumentTagList(anyInt(), anyInt())).thenReturn(emptyPage);
            when(contentApi.contentDocumentTagCreate(any())).thenReturn(created);

            StepVerifier.create(service.upsertContentTags(List.of(tag)))
                .verifyComplete();

            verify(contentApi, times(1)).contentDocumentTagCreate(any());
            verify(contentApi, never()).contentDocumentTagPartialUpdate(any(), any());
        }

        @Test
        @DisplayName("existing tag triggers patch")
        void existingTagTriggersPatch() {
            ContentTag tag = new ContentTag("docCode1", "newValue");
            DocumentTag existingTag = new DocumentTag().code("docCode1").value("oldValue");
            PaginatedDocumentTagList page = new PaginatedDocumentTagList().results(List.of(existingTag));
            DocumentTag patched = new DocumentTag().code("docCode1").value("newValue");

            when(contentApi.contentDocumentTagList(anyInt(), anyInt())).thenReturn(page);
            when(contentApi.contentDocumentTagPartialUpdate(anyString(), any())).thenReturn(patched);

            StepVerifier.create(service.upsertContentTags(List.of(tag)))
                .verifyComplete();

            verify(contentApi, times(1)).contentDocumentTagPartialUpdate(anyString(), any());
            verify(contentApi, never()).contentDocumentTagCreate(any());
        }

        @Test
        @DisplayName("API failure on tag create is swallowed and processing continues")
        void apiFailureOnTagCreateIsSwallowed() {
            ContentTag tag1 = new ContentTag("code1", "value1");
            ContentTag tag2 = new ContentTag("code2", "value2");
            PaginatedDocumentTagList emptyPage = new PaginatedDocumentTagList().results(List.of());
            DocumentTag created = new DocumentTag().code("code2").value("value2");

            when(contentApi.contentDocumentTagList(anyInt(), anyInt())).thenReturn(emptyPage);
            when(contentApi.contentDocumentTagCreate(any()))
                .thenThrow(new RuntimeException("API error"))
                .thenReturn(created);

            StepVerifier.create(service.upsertContentTags(List.of(tag1, tag2)))
                .verifyComplete();

            verify(contentApi, times(2)).contentDocumentTagCreate(any());
        }

        @Test
        @DisplayName("multiple tags: existing gets patched, new gets created")
        void multipleTagsMixedUpsert() {
            ContentTag existTag = new ContentTag("code1", "v1");
            ContentTag newTag = new ContentTag("code2", "v2");
            DocumentTag existing = new DocumentTag().code("code1").value("oldV1");
            PaginatedDocumentTagList page = new PaginatedDocumentTagList().results(List.of(existing));
            DocumentTag patched = new DocumentTag().code("code1").value("v1");
            DocumentTag created = new DocumentTag().code("code2").value("v2");

            when(contentApi.contentDocumentTagList(anyInt(), anyInt())).thenReturn(page);
            when(contentApi.contentDocumentTagPartialUpdate(anyString(), any())).thenReturn(patched);
            when(contentApi.contentDocumentTagCreate(any())).thenReturn(created);

            StepVerifier.create(service.upsertContentTags(List.of(existTag, newTag)))
                .verifyComplete();

            verify(contentApi, times(1)).contentDocumentTagPartialUpdate(anyString(), any());
            verify(contentApi, times(1)).contentDocumentTagCreate(any());
        }
    }

    // -----------------------------------------------------------------------
    // upsertDocuments
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("upsertDocuments")
    class UpsertDocuments {

        @Test
        @DisplayName("empty list completes without calling API")
        void emptyListCompletesWithoutApiCall() {
            PaginatedOASDocumentResponseList emptyPage = new PaginatedOASDocumentResponseList().results(List.of());
            when(contentApi.listContentDocuments(isNull(), anyInt(), isNull(), anyInt(), isNull(), isNull()))
                .thenReturn(emptyPage);

            StepVerifier.create(service.upsertDocuments(List.of()))
                .verifyComplete();

            verify(apiClient, never()).invokeAPI(anyString(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any());
        }

        @Test
        @DisplayName("no existing documents - all documents are inserted via apiClient")
        void noExistingDocumentsAllInserted() {
            final ContentDocumentEntry doc = ContentDocumentEntry.builder().name("Doc 1").build();
            PaginatedOASDocumentResponseList emptyPage = new PaginatedOASDocumentResponseList().results(List.of());
            // OASDocumentResponse: uuid is constructor-only; name has setName()
            OASDocumentResponse created = new OASDocumentResponse(UUID.randomUUID());
            created.setName("Doc 1");

            when(contentApi.listContentDocuments(isNull(), anyInt(), isNull(), anyInt(), isNull(), isNull()))
                .thenReturn(emptyPage);
            when(apiClient.invokeAPI(anyString(), any(), any(), any(), isNull(), any(), any(), any(), any(), any(),
                any(), any())).thenReturn(new ResponseEntity<>(created, HttpStatus.OK));

            StepVerifier.create(service.upsertDocuments(List.of(doc)))
                .verifyComplete();
        }

        @Test
        @DisplayName("existing document by name triggers patch via apiClient")
        void existingDocumentTriggersPatch() {
            UUID existingUuid = UUID.randomUUID();
            final ContentDocumentEntry doc = ContentDocumentEntry.builder().name("Existing Doc").build();
            OASDocumentResponse existingResponse = new OASDocumentResponse(existingUuid);
            existingResponse.setName("Existing Doc");
            PaginatedOASDocumentResponseList page = new PaginatedOASDocumentResponseList()
                .results(List.of(existingResponse));
            OASDocumentResponse patched = new OASDocumentResponse(existingUuid);
            patched.setName("Existing Doc");

            when(contentApi.listContentDocuments(isNull(), anyInt(), isNull(), anyInt(), isNull(), isNull()))
                .thenReturn(page);
            when(apiClient.invokeAPI(anyString(), any(), any(), any(), isNull(), any(), any(), any(), any(), any(),
                any(), any())).thenReturn(new ResponseEntity<>(patched, HttpStatus.OK));

            StepVerifier.create(service.upsertDocuments(List.of(doc)))
                .verifyComplete();
        }

        @Test
        @DisplayName("API failure during document insert is swallowed and processing continues")
        void apiFailureIsSwallowedAndProcessingContinues() {
            final ContentDocumentEntry doc1 = ContentDocumentEntry.builder().name("Doc 1").build();
            final ContentDocumentEntry doc2 = ContentDocumentEntry.builder().name("Doc 2").build();
            PaginatedOASDocumentResponseList emptyPage = new PaginatedOASDocumentResponseList().results(List.of());
            OASDocumentResponse created = new OASDocumentResponse(UUID.randomUUID());
            created.setName("Doc 2");

            when(contentApi.listContentDocuments(isNull(), anyInt(), isNull(), anyInt(), isNull(), isNull()))
                .thenReturn(emptyPage);
            when(apiClient.invokeAPI(anyString(), any(), any(), any(), isNull(), any(), any(), any(), any(), any(),
                any(), any()))
                .thenThrow(new RuntimeException("API failure"))
                .thenReturn(new ResponseEntity<>(created, HttpStatus.OK));

            StepVerifier.create(service.upsertDocuments(List.of(doc1, doc2)))
                .verifyComplete();
        }
    }
}

