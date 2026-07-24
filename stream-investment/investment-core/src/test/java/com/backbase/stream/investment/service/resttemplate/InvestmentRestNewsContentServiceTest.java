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

/**
 * Unit tests for {@link InvestmentRestNewsContentService}.
 *
 * <p>Verifies tag upsert via generated {@code ContentApi} clients and content entry create via
 * manual {@code ApiClient} calls (JSON POST with {@code byte[]} body, optional multipart thumbnail PATCH).
 */
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
            eq(Map.of("uuid", uuid.toString())),
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
            org.junit.jupiter.params.provider.Arguments.of("  ", "someValue"),
            org.junit.jupiter.params.provider.Arguments.of("code1", null),
            org.junit.jupiter.params.provider.Arguments.of("code1", "  ")
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
        }

        @ParameterizedTest(name = "tag skipped when code=''{0}'' value=''{1}''")
        @MethodSource("com.backbase.stream.investment.service.resttemplate.InvestmentRestNewsContentServiceTest#invalidTags")
        void tagWithInvalidCodeOrValueIsSkipped(String code, String value) {
            ContentTag tag = new ContentTag(code, value);

            StepVerifier.create(service.upsertTags(List.of(tag)))
                .verifyComplete();

            verify(contentApi, never()).contentEntryTagList(anyInt(), anyInt());
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
        }
    }

    // -----------------------------------------------------------------------
    // upsertContent
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("upsertContent")
    class UpsertContent {

        @Test
        @DisplayName("no thumbnail uses JSON POST without thumbnail field")
        void noThumbnailUsesJsonPost() throws Exception {
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

            byte[] bodyBytes = (byte[]) jsonBodyCaptor.getValue();
            JsonNode body = objectMapper.readTree(bodyBytes);
            assertThat(body.has(JSON_PROPERTY_THUMBNAIL)).isFalse();
            assertThat(body.get(JSON_PROPERTY_ASSETS).isArray()).isTrue();
            assertThat(body.get(JSON_PROPERTY_ASSETS)).isEmpty();
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
                eq("/service-api/v2/content/entries/{uuid}/"), eq(HttpMethod.PATCH), eq(Map.of("uuid", createdUuid.toString())),
                any(), isNull(), any(), any(), any(), any(), eq(MediaType.MULTIPART_FORM_DATA), any(), any());
            assertThat(formParamsCaptor.getValue().get(JSON_PROPERTY_THUMBNAIL)).hasSize(1);
            assertThat(formParamsCaptor.getValue().get(JSON_PROPERTY_ASSETS)).isNull();
        }

        @Test
        @DisplayName("existing entries with matching title are skipped")
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
    }
}
