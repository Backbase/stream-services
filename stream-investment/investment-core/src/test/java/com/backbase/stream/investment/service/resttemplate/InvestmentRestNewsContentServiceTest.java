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
import com.backbase.investment.api.service.sync.v1.model.Entry;
import com.backbase.investment.api.service.sync.v1.model.EntryCreateUpdate;
import com.backbase.investment.api.service.sync.v1.model.EntryTag;
import com.backbase.investment.api.service.sync.v1.model.PaginatedEntryList;
import com.backbase.investment.api.service.sync.v1.model.PaginatedEntryTagList;
import com.backbase.stream.investment.model.ContentTag;
import com.backbase.stream.investment.model.MarketNewsEntry;
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
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class InvestmentRestNewsContentServiceTest {

    @Mock
    private ContentApi contentApi;

    @Mock
    private ApiClient apiClient;

    private InvestmentRestNewsContentService service;

    @BeforeEach
    void setUp() {
        service = new InvestmentRestNewsContentService(contentApi, apiClient);
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

            verify(contentApi, never()).createContentEntry(any());
        }

        @Test
        @DisplayName("no existing entries - all entries are created")
        void noExistingEntriesAllCreated() {
            MarketNewsEntry entry = new MarketNewsEntry();
            entry.setTitle("News Title");

            PaginatedEntryList emptyPage = new PaginatedEntryList().results(List.of());
            // EntryCreateUpdate: uuid/assets/createdBy are constructor-only; title has setTitle()
            EntryCreateUpdate created = new EntryCreateUpdate(UUID.randomUUID(), null, null);
            created.setTitle("News Title");

            when(contentApi.listContentEntries(isNull(), anyInt(), anyInt(),
                isNull(), isNull(), isNull(), isNull())).thenReturn(emptyPage);
            when(contentApi.createContentEntry(any())).thenReturn(created);

            StepVerifier.create(service.upsertContent(List.of(entry)))
                .verifyComplete();

            verify(contentApi, times(1)).createContentEntry(any());
        }

        @Test
        @DisplayName("existing entries with matching title are skipped (not duplicated)")
        void existingEntriesWithMatchingTitleAreSkipped() {
            MarketNewsEntry entry = new MarketNewsEntry();
            entry.setTitle("Existing News");

            // Entry: uuid and title are constructor-only (both in @JsonCreator)
            Entry existingEntry = new Entry(UUID.randomUUID(), "Existing News",
                null, null, null, null, null, null, null, null);
            PaginatedEntryList page = new PaginatedEntryList().results(List.of(existingEntry));

            when(contentApi.listContentEntries(isNull(), anyInt(), anyInt(),
                isNull(), isNull(), isNull(), isNull())).thenReturn(page);

            StepVerifier.create(service.upsertContent(List.of(entry)))
                .verifyComplete();

            verify(contentApi, never()).createContentEntry(any());
        }

        @Test
        @DisplayName("only new entries (not matching existing) are created")
        void onlyNewEntriesAreCreated() {
            MarketNewsEntry existingEntry = new MarketNewsEntry();
            existingEntry.setTitle("Existing News");
            MarketNewsEntry newEntry = new MarketNewsEntry();
            newEntry.setTitle("Brand New News");

            Entry serverEntry = new Entry(UUID.randomUUID(), "Existing News",
                null, null, null, null, null, null, null, null);
            PaginatedEntryList page = new PaginatedEntryList().results(List.of(serverEntry));
            EntryCreateUpdate created = new EntryCreateUpdate(UUID.randomUUID(), null, null);
            created.setTitle("Brand New News");

            when(contentApi.listContentEntries(isNull(), anyInt(), anyInt(),
                isNull(), isNull(), isNull(), isNull())).thenReturn(page);
            when(contentApi.createContentEntry(any())).thenReturn(created);

            StepVerifier.create(service.upsertContent(List.of(existingEntry, newEntry)))
                .verifyComplete();

            verify(contentApi, times(1)).createContentEntry(any());
        }

        @Test
        @DisplayName("API failure on entry create is swallowed and processing continues")
        void apiFailureOnCreateIsSwallowed() {
            MarketNewsEntry entry1 = new MarketNewsEntry();
            entry1.setTitle("News 1");
            MarketNewsEntry entry2 = new MarketNewsEntry();
            entry2.setTitle("News 2");

            PaginatedEntryList emptyPage = new PaginatedEntryList().results(List.of());
            EntryCreateUpdate created = new EntryCreateUpdate(UUID.randomUUID(), null, null);
            created.setTitle("News 2");

            when(contentApi.listContentEntries(isNull(), anyInt(), anyInt(),
                isNull(), isNull(), isNull(), isNull())).thenReturn(emptyPage);
            when(contentApi.createContentEntry(any()))
                .thenThrow(new RuntimeException("API failure"))
                .thenReturn(created);

            StepVerifier.create(service.upsertContent(List.of(entry1, entry2)))
                .verifyComplete();

            verify(contentApi, times(2)).createContentEntry(any());
        }

        @Test
        @DisplayName("entry without thumbnail skips thumbnail PATCH call via apiClient")
        void entryWithoutThumbnailSkipsThumbnailAttachment() {
            MarketNewsEntry entry = new MarketNewsEntry();
            entry.setTitle("No Thumbnail News");
            // thumbnailResource is null by default

            PaginatedEntryList emptyPage = new PaginatedEntryList().results(List.of());
            EntryCreateUpdate created = new EntryCreateUpdate(UUID.randomUUID(), null, null);
            created.setTitle("No Thumbnail News");

            when(contentApi.listContentEntries(isNull(), anyInt(), anyInt(),
                isNull(), isNull(), isNull(), isNull())).thenReturn(emptyPage);
            when(contentApi.createContentEntry(any())).thenReturn(created);

            StepVerifier.create(service.upsertContent(List.of(entry)))
                .verifyComplete();

            // No thumbnail PATCH via apiClient
            verify(apiClient, never()).invokeAPI(anyString(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any());
        }
    }
}

