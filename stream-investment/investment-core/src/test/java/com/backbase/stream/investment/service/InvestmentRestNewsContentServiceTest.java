package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.sync.ApiClient;
import com.backbase.investment.api.service.sync.v1.ContentApi;
import com.backbase.investment.api.service.v1.model.EntryCreateUpdate;
import com.backbase.investment.api.service.v1.model.EntryCreateUpdateRequest;
import com.backbase.investment.api.service.v1.model.PaginatedEntryList;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestNewsContentService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InvestmentRestNewsContentServiceTest {

    private ContentApi contentApi;
    private ApiClient apiClient;
    private InvestmentRestNewsContentService service;

    @BeforeEach
    void setUp() {
        contentApi = Mockito.mock(ContentApi.class);
        apiClient = Mockito.mock(ApiClient.class);
        service = new InvestmentRestNewsContentService(contentApi, apiClient);
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

    }


}

