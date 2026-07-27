package com.backbase.stream.investment.service.resttemplate;

import static com.backbase.investment.api.service.sync.v1.model.EntryCreateUpdateRequest.JSON_PROPERTY_ASSETS;
import static com.backbase.investment.api.service.sync.v1.model.EntryCreateUpdateRequest.JSON_PROPERTY_THUMBNAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.sync.ApiClient;
import com.backbase.investment.api.service.sync.v1.ContentApi;
import com.backbase.investment.api.service.sync.v1.model.Entry;
import com.backbase.investment.api.service.sync.v1.model.EntryCreateUpdate;
import com.backbase.investment.api.service.sync.v1.model.EntryTag;
import com.backbase.investment.api.service.sync.v1.model.PaginatedEntryList;
import com.backbase.investment.api.service.sync.v1.model.PaginatedEntryTagList;
import com.backbase.stream.investment.model.ContentTag;
import com.backbase.stream.investment.model.MarketNewsEntry;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class InvestmentRestNewsContentServiceTest {

    @Mock
    private ContentApi contentApi;

    @Mock
    private ApiClient apiClient;

    @Captor
    private ArgumentCaptor<Object> jsonBodyCaptor;

    @Captor
    private ArgumentCaptor<MultiValueMap<String, Object>> formParamsCaptor;

    private ObjectMapper objectMapper;
    private InvestmentRestNewsContentService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        service = new InvestmentRestNewsContentService(contentApi, apiClient, objectMapper);
    }

    private void stubJsonContentEntryCreate(EntryCreateUpdate created) {
        when(apiClient.selectHeaderAccept(any())).thenReturn(List.of(MediaType.APPLICATION_JSON));
        when(apiClient.selectHeaderContentType(new String[]{"application/json"})).thenReturn(MediaType.APPLICATION_JSON);
        when(apiClient.invokeAPI(
            eq("/service-api/v2/content/entries/"),
            eq(HttpMethod.POST),
            any(),
            any(),
            jsonBodyCaptor.capture(),
            any(),
            any(),
            any(),
            any(),
            eq(MediaType.APPLICATION_JSON),
            any(),
            any())).thenReturn(ResponseEntity.ok(created));
    }

    private void stubMultipartContentEntryPatch(UUID uuid, EntryCreateUpdate patched) {
        when(apiClient.selectHeaderAccept(any())).thenReturn(List.of(MediaType.APPLICATION_JSON));
        when(apiClient.selectHeaderContentType(new String[]{"multipart/form-data"}))
            .thenReturn(MediaType.MULTIPART_FORM_DATA);
        when(apiClient.invokeAPI(
            eq("/service-api/v2/content/entries/{uuid}/"),
            eq(HttpMethod.PATCH),
            eq(Map.of("uuid", uuid)),
            any(),
            isNull(),
            any(),
            any(),
            formParamsCaptor.capture(),
            any(),
            eq(MediaType.MULTIPART_FORM_DATA),
            any(),
            any())).thenReturn(ResponseEntity.ok(patched));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> invalidTags() {
        return Stream.of(
            org.junit.jupiter.params.provider.Arguments.of("  ", "someValue"),  // blank code
            org.junit.jupiter.params.provider.Arguments.of("code1", null),      // null value
            org.junit.jupiter.params.provider.Arguments.of("code1", "  ")       // blank value
        );
    }

    // -----------------------------------------------------------------------
    // upsertTags
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("upsertTags")
    class UpsertTags {

        @Test
        @DisplayName("empty list completes without calling API")
        void emptyListCompletesWithoutApiCall() {
            StepVerifier.create(service.upsertTags(List.of()))
                .verifyComplete();

            verify(contentApi, never()).contentEntryTagList(anyInt(), anyInt());
        }

        @Test
        @DisplayName("tag with null code is skipped")
        void tagWithNullCodeIsSkipped() {
            ContentTag tag = new ContentTag(null, "someValue");

            StepVerifier.create(service.upsertTags(List.of(tag)))
                .verifyComplete();

            verify(contentApi, never()).contentEntryTagList(anyInt(), anyInt());
            verify(contentApi, never()).contentEntryTagCreate(any());
            verify(contentApi, never()).contentEntryTagPartialUpdate(any(), any());
        }

        @ParameterizedTest(name = "tag skipped when code=''{0}'' value=''{1}''")
        @DisplayName("tag with blank/null code or blank/null value is skipped")
        @MethodSource("com.backbase.stream.investment.service.resttemplate.InvestmentRestNewsContentServiceTest#invalidTags")
        void tagWithInvalidCodeOrValueIsSkipped(String code, String value) {
            ContentTag tag = new ContentTag(code, value);

            StepVerifier.create(service.upsertTags(List.of(tag)))
                .verifyComplete();

            verify(contentApi, never()).contentEntryTagList(anyInt(), anyInt());
            verify(contentApi, never()).contentEntryTagCreate(any());
        }

        @Test
        @DisplayName("new tag (not existing) triggers create")
        void newTagTriggersCreate() {
            ContentTag tag = new ContentTag("code1", "value1");
            PaginatedEntryTagList emptyPage = new PaginatedEntryTagList().results(List.of());
            EntryTag createdTag = new EntryTag().code("code1").value("value1");

            when(contentApi.contentEntryTagList(anyInt(), anyInt())).thenReturn(emptyPage);
            when(contentApi.contentEntryTagCreate(any())).thenReturn(createdTag);

            StepVerifier.create(service.upsertTags(List.of(tag)))
                .verifyComplete();

            verify(contentApi, times(1)).contentEntryTagCreate(any());
            verify(contentApi, never()).contentEntryTagPartialUpdate(any(), any());
        }

        @Test
        @DisplayName("existing tag triggers patch")
        void existingTagTriggersPatch() {
            ContentTag tag = new ContentTag("code1", "value1");
            EntryTag existingTag = new EntryTag().code("code1").value("oldValue");
            PaginatedEntryTagList page = new PaginatedEntryTagList().results(List.of(existingTag));
            EntryTag patchedTag = new EntryTag().code("code1").value("value1");

            when(contentApi.contentEntryTagList(anyInt(), anyInt())).thenReturn(page);
            when(contentApi.contentEntryTagPartialUpdate(anyString(), any())).thenReturn(patchedTag);

            StepVerifier.create(service.upsertTags(List.of(tag)))
                .verifyComplete();

            verify(contentApi, times(1)).contentEntryTagPartialUpdate(anyString(), any());
            verify(contentApi, never()).contentEntryTagCreate(any());
        }

        @Test
        @DisplayName("API failure on tag create is swallowed and processing continues")
        void apiFailureOnTagCreateIsSwallowed() {
            ContentTag tag1 = new ContentTag("code1", "value1");
            ContentTag tag2 = new ContentTag("code2", "value2");
            PaginatedEntryTagList emptyPage = new PaginatedEntryTagList().results(List.of());
            EntryTag createdTag2 = new EntryTag().code("code2").value("value2");

            when(contentApi.contentEntryTagList(anyInt(), anyInt())).thenReturn(emptyPage);
            when(contentApi.contentEntryTagCreate(any()))
                .thenThrow(new RuntimeException("API error"))
                .thenReturn(createdTag2);

            StepVerifier.create(service.upsertTags(List.of(tag1, tag2)))
                .verifyComplete();

            verify(contentApi, times(2)).contentEntryTagCreate(any());
        }

        @Test
        @DisplayName("multiple tags: existing gets patched, new gets created")
        void multipleTagsAllProcessed() {
            ContentTag tag1 = new ContentTag("code1", "value1");
            ContentTag tag2 = new ContentTag("code2", "value2");
            EntryTag existingTag1 = new EntryTag().code("code1").value("old1");
            PaginatedEntryTagList page = new PaginatedEntryTagList().results(List.of(existingTag1));
            EntryTag patchedTag = new EntryTag().code("code1").value("value1");
            EntryTag createdTag = new EntryTag().code("code2").value("value2");

            when(contentApi.contentEntryTagList(anyInt(), anyInt())).thenReturn(page);
            when(contentApi.contentEntryTagPartialUpdate(anyString(), any())).thenReturn(patchedTag);
            when(contentApi.contentEntryTagCreate(any())).thenReturn(createdTag);

            StepVerifier.create(service.upsertTags(List.of(tag1, tag2)))
                .verifyComplete();

            verify(contentApi, times(1)).contentEntryTagPartialUpdate(anyString(), any());
            verify(contentApi, times(1)).contentEntryTagCreate(any());
        }
    }

    // -----------------------------------------------------------------------
    // upsertContent
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("upsertContent")
    class UpsertContent {

        @Test
        @DisplayName("empty list completes without calling API")
        void emptyListCompletesWithoutApiCall() {
            PaginatedEntryList emptyPage = new PaginatedEntryList().results(List.of());
            when(contentApi.listContentEntries(isNull(), anyInt(), anyInt(),
                isNull(), isNull(), isNull(), isNull())).thenReturn(emptyPage);

            StepVerifier.create(service.upsertContent(List.of()))
                .verifyComplete();

            verify(apiClient, never()).invokeAPI(
                eq("/service-api/v2/content/entries/"), eq(HttpMethod.POST), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any());
        }

        @Test
        @DisplayName("no existing entries - all entries are created via JSON POST")
        void noExistingEntriesAllCreated() throws Exception {
            MarketNewsEntry entry = new MarketNewsEntry();
            entry.setTitle("News Title");
            entry.setTags(List.of("INVESTOR_AREA"));

            PaginatedEntryList emptyPage = new PaginatedEntryList().results(List.of());
            EntryCreateUpdate created = new EntryCreateUpdate(UUID.randomUUID(), null, null);
            created.setTitle("News Title");

            when(contentApi.listContentEntries(isNull(), anyInt(), anyInt(),
                isNull(), isNull(), isNull(), isNull())).thenReturn(emptyPage);
            stubJsonContentEntryCreate(created);

            StepVerifier.create(service.upsertContent(List.of(entry)))
                .verifyComplete();

            verify(apiClient, times(1)).invokeAPI(
                eq("/service-api/v2/content/entries/"), eq(HttpMethod.POST), any(), any(), any(), any(), any(), any(),
                any(), eq(MediaType.APPLICATION_JSON), any(), any());

            byte[] bodyBytes = (byte[]) jsonBodyCaptor.getValue();
            JsonNode body = objectMapper.readTree(bodyBytes);
            assertThat(body.has(JSON_PROPERTY_THUMBNAIL)).isFalse();
            assertThat(body.get(JSON_PROPERTY_ASSETS).isArray()).isTrue();
            assertThat(body.get(JSON_PROPERTY_ASSETS)).isEmpty();
        }

        @Test
        @DisplayName("existing entries with matching title are skipped (not duplicated)")
        void existingEntriesWithMatchingTitleAreSkipped() {
            MarketNewsEntry entry = new MarketNewsEntry();
            entry.setTitle("Existing News");

            Entry existingEntry = new Entry(UUID.randomUUID(), "Existing News",
                null, null, null, null, null, null, null, null);
            PaginatedEntryList page = new PaginatedEntryList().results(List.of(existingEntry));

            when(contentApi.listContentEntries(isNull(), anyInt(), anyInt(),
                isNull(), isNull(), isNull(), isNull())).thenReturn(page);

            StepVerifier.create(service.upsertContent(List.of(entry)))
                .verifyComplete();

            verify(apiClient, never()).invokeAPI(
                eq("/service-api/v2/content/entries/"), eq(HttpMethod.POST), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any());
        }

        @Test
        @DisplayName("only new entries (not matching existing) are created")
        void onlyNewEntriesAreCreated() {
            MarketNewsEntry existingEntry = new MarketNewsEntry();
            existingEntry.setTitle("Existing News");
            MarketNewsEntry newEntry = new MarketNewsEntry();
            newEntry.setTitle("Brand New News");
            newEntry.setTags(List.of("INVESTOR_AREA"));

            Entry serverEntry = new Entry(UUID.randomUUID(), "Existing News",
                null, null, null, null, null, null, null, null);
            PaginatedEntryList page = new PaginatedEntryList().results(List.of(serverEntry));
            EntryCreateUpdate created = new EntryCreateUpdate(UUID.randomUUID(), null, null);
            created.setTitle("Brand New News");

            when(contentApi.listContentEntries(isNull(), anyInt(), anyInt(),
                isNull(), isNull(), isNull(), isNull())).thenReturn(page);
            stubJsonContentEntryCreate(created);

            StepVerifier.create(service.upsertContent(List.of(existingEntry, newEntry)))
                .verifyComplete();

            verify(apiClient, times(1)).invokeAPI(
                eq("/service-api/v2/content/entries/"), eq(HttpMethod.POST), any(), any(), any(), any(), any(), any(),
                any(), eq(MediaType.APPLICATION_JSON), any(), any());
        }

        @Test
        @DisplayName("API failure on entry create is swallowed and processing continues")
        void apiFailureOnCreateIsSwallowed() {
            MarketNewsEntry entry1 = new MarketNewsEntry();
            entry1.setTitle("News 1");
            entry1.setTags(List.of("INVESTOR_AREA"));
            MarketNewsEntry entry2 = new MarketNewsEntry();
            entry2.setTitle("News 2");
            entry2.setTags(List.of("INVESTOR_AREA"));

            PaginatedEntryList emptyPage = new PaginatedEntryList().results(List.of());
            EntryCreateUpdate created = new EntryCreateUpdate(UUID.randomUUID(), null, null);
            created.setTitle("News 2");

            when(contentApi.listContentEntries(isNull(), anyInt(), anyInt(),
                isNull(), isNull(), isNull(), isNull())).thenReturn(emptyPage);
            when(apiClient.selectHeaderAccept(any())).thenReturn(List.of(MediaType.APPLICATION_JSON));
            when(apiClient.selectHeaderContentType(new String[]{"application/json"})).thenReturn(MediaType.APPLICATION_JSON);
            when(apiClient.invokeAPI(
                eq("/service-api/v2/content/entries/"),
                eq(HttpMethod.POST),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(MediaType.APPLICATION_JSON),
                any(),
                any()))
                .thenThrow(new RuntimeException("API failure"))
                .thenReturn(ResponseEntity.ok(created));

            StepVerifier.create(service.upsertContent(List.of(entry1, entry2)))
                .verifyComplete();

            verify(apiClient, times(2)).invokeAPI(
                eq("/service-api/v2/content/entries/"), eq(HttpMethod.POST), any(), any(), any(), any(), any(), any(),
                any(), eq(MediaType.APPLICATION_JSON), any(), any());
        }

        @Test
        @DisplayName("entry without thumbnail skips thumbnail PATCH call via apiClient")
        void entryWithoutThumbnailSkipsThumbnailAttachment() {
            MarketNewsEntry entry = new MarketNewsEntry();
            entry.setTitle("No Thumbnail News");
            entry.setTags(List.of("INVESTOR_AREA"));

            PaginatedEntryList emptyPage = new PaginatedEntryList().results(List.of());
            EntryCreateUpdate created = new EntryCreateUpdate(UUID.randomUUID(), null, null);
            created.setTitle("No Thumbnail News");

            when(contentApi.listContentEntries(isNull(), anyInt(), anyInt(),
                isNull(), isNull(), isNull(), isNull())).thenReturn(emptyPage);
            stubJsonContentEntryCreate(created);

            StepVerifier.create(service.upsertContent(List.of(entry)))
                .verifyComplete();

            verify(apiClient, times(1)).invokeAPI(
                eq("/service-api/v2/content/entries/"), eq(HttpMethod.POST), any(), any(), any(), any(), any(), any(),
                any(), eq(MediaType.APPLICATION_JSON), any(), any());
            verify(apiClient, never()).invokeAPI(
                eq("/service-api/v2/content/entries/{uuid}/"), eq(HttpMethod.PATCH), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("thumbnail resource uses JSON create then multipart PATCH for thumbnail")
        void thumbnailUsesJsonCreateThenMultipartPatch() {
            MarketNewsEntry entry = new MarketNewsEntry();
            entry.setTitle("News Title");
            entry.setTags(List.of("INVESTOR_AREA"));
            entry.setThumbnailResource(new ByteArrayResource("image".getBytes(), "example.png"));

            PaginatedEntryList emptyPage = new PaginatedEntryList().results(List.of());
            UUID createdUuid = UUID.randomUUID();
            EntryCreateUpdate created = new EntryCreateUpdate(createdUuid, null, null);
            created.setTitle("News Title");
            EntryCreateUpdate patched = new EntryCreateUpdate(createdUuid, null, null);
            patched.setTitle("News Title");

            when(contentApi.listContentEntries(isNull(), anyInt(), anyInt(),
                isNull(), isNull(), isNull(), isNull())).thenReturn(emptyPage);
            stubJsonContentEntryCreate(created);
            stubMultipartContentEntryPatch(createdUuid, patched);

            StepVerifier.create(service.upsertContent(List.of(entry)))
                .verifyComplete();

            verify(apiClient, times(1)).invokeAPI(
                eq("/service-api/v2/content/entries/"), eq(HttpMethod.POST), any(), any(), any(), any(), any(), any(),
                any(), eq(MediaType.APPLICATION_JSON), any(), any());
            verify(apiClient, times(1)).invokeAPI(
                eq("/service-api/v2/content/entries/{uuid}/"), eq(HttpMethod.PATCH),
                eq(Map.of("uuid", createdUuid)), any(), isNull(), any(), any(), any(), any(),
                eq(MediaType.MULTIPART_FORM_DATA), any(), any());
            assertThat(formParamsCaptor.getValue().get(JSON_PROPERTY_THUMBNAIL)).hasSize(1);
            assertThat(formParamsCaptor.getValue().get(JSON_PROPERTY_ASSETS)).isNull();
        }

        @Test
        @DisplayName("thumbnail PATCH failure retains created entry")
        void thumbnailPatchFailureRetainsCreatedEntry() {
            MarketNewsEntry entry = new MarketNewsEntry();
            entry.setTitle("News Title");
            entry.setTags(List.of("INVESTOR_AREA"));
            entry.setThumbnailResource(new ByteArrayResource("image".getBytes(), "example.png"));

            PaginatedEntryList emptyPage = new PaginatedEntryList().results(List.of());
            UUID createdUuid = UUID.randomUUID();
            EntryCreateUpdate created = new EntryCreateUpdate(createdUuid, null, null);
            created.setTitle("News Title");

            when(contentApi.listContentEntries(isNull(), anyInt(), anyInt(),
                isNull(), isNull(), isNull(), isNull())).thenReturn(emptyPage);
            stubJsonContentEntryCreate(created);
            when(apiClient.selectHeaderContentType(new String[]{"multipart/form-data"}))
                .thenReturn(MediaType.MULTIPART_FORM_DATA);
            when(apiClient.invokeAPI(
                eq("/service-api/v2/content/entries/{uuid}/"),
                eq(HttpMethod.PATCH),
                eq(Map.of("uuid", createdUuid)),
                any(),
                isNull(),
                any(),
                any(),
                any(),
                any(),
                eq(MediaType.MULTIPART_FORM_DATA),
                any(),
                any())).thenThrow(new RuntimeException("thumbnail upload failed"));

            StepVerifier.create(service.upsertContent(List.of(entry)))
                .verifyComplete();

            verify(apiClient, times(1)).invokeAPI(
                eq("/service-api/v2/content/entries/"), eq(HttpMethod.POST), any(), any(), any(), any(), any(), any(),
                any(), eq(MediaType.APPLICATION_JSON), any(), any());
            verify(apiClient, times(1)).invokeAPI(
                eq("/service-api/v2/content/entries/{uuid}/"), eq(HttpMethod.PATCH),
                eq(Map.of("uuid", createdUuid)), any(), isNull(), any(), any(), any(), any(),
                eq(MediaType.MULTIPART_FORM_DATA), any(), any());
        }
    }
}
