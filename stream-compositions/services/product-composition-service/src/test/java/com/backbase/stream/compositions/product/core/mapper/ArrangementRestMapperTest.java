package com.backbase.stream.compositions.product.core.mapper;

import com.backbase.stream.compositions.product.api.model.*;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestResponse;
import com.backbase.stream.compositions.product.core.model.RequestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArrangementRestMapperTest {
    private static final String ARRANGEMENT_ID = "arrangementId";
    private static final String EXTERNAL_ARRANGEMENT_ID = "externalArrangementId";
    private static final String SOURCE = "source";

    @InjectMocks
    ArrangementRestMapper arrangementRestMapper;

    @Mock
    ArrangementMapper arrangementMapper;

    @Mock
    ConfigMapper chainsMapper;

    @Test
    void mapPushRequest() {
        AccountArrangementItemPut arrangementItemPut = new AccountArrangementItemPut();

        when(arrangementMapper.mapCompositionToStream(arrangementItemPut))
                .thenReturn(new com.backbase.dbs.arrangement.api.service.v3.model.ArrangementPutItem());

        when(chainsMapper.map(any()))
                .thenReturn(RequestConfig.builder().build());

        ArrangementIngestionConfig requestConfig = new ArrangementIngestionConfig();

        ArrangementPushIngestionRequest request =
                new ArrangementPushIngestionRequest()
                        .internalArrangementId(ARRANGEMENT_ID)
                        .source(SOURCE)
                        .arrangement(arrangementItemPut)
                        .config(requestConfig);

        ArrangementIngestPushRequest mappedRequest = arrangementRestMapper.mapPushRequest(request);
        assertEquals(SOURCE, mappedRequest.getSource());
        assertEquals(ARRANGEMENT_ID, mappedRequest.getArrangementInternalId());
        assertNotNull(mappedRequest.getConfig());
    }

    @Test
    void mapPullRequest() {
        when(chainsMapper.map(any()))
                .thenReturn(RequestConfig.builder().build());

        ArrangementIngestionConfig requestConfig = new ArrangementIngestionConfig();

        ArrangementPullIngestionRequest request =
                new ArrangementPullIngestionRequest()
                        .internalArrangementId(ARRANGEMENT_ID)
                        .externalArrangementId(EXTERNAL_ARRANGEMENT_ID)
                        .source(SOURCE)
                        .config(requestConfig);

        ArrangementIngestPullRequest mappedRequest = arrangementRestMapper.mapPullRequest(request);
        assertEquals(SOURCE, mappedRequest.getSource());
        assertEquals(ARRANGEMENT_ID, mappedRequest.getArrangementId());
        assertNotNull(mappedRequest.getConfig());
    }

    @Test
    void mapResponse() {
        com.backbase.dbs.arrangement.api.service.v3.model.ArrangementPutItem streamArrangement =
                new com.backbase.dbs.arrangement.api.service.v3.model.ArrangementPutItem();

        AccountArrangementItemPut compositionArrangement = new AccountArrangementItemPut();

        when(arrangementMapper.mapStreamToComposition(streamArrangement))
                .thenReturn(compositionArrangement);

        ArrangementIngestResponse response = ArrangementIngestResponse
                .builder()
                .arrangement(new com.backbase.dbs.arrangement.api.service.v3.model.ArrangementPutItem())
                .build();

        ResponseEntity<ArrangementIngestionResponse> responseEntity = arrangementRestMapper.mapResponse(response);

        assertNotNull(responseEntity);
        assertEquals(compositionArrangement, responseEntity.getBody().getArrangement());
    }
}
