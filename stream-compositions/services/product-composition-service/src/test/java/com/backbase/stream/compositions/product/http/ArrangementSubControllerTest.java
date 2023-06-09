package com.backbase.stream.compositions.product.http;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.product.api.model.ArrangementIngestionResponse;
import com.backbase.stream.compositions.product.api.model.ArrangementPullIngestionRequest;
import com.backbase.stream.compositions.product.api.model.ArrangementPushIngestionRequest;
import com.backbase.stream.compositions.product.core.mapper.ArrangementRestMapper;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestResponse;
import com.backbase.stream.compositions.product.core.service.ArrangementIngestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ArrangementSubControllerTest {

    @InjectMocks
    ArrangementSubController arrangementSubController;

    @Mock
    ArrangementIngestionService arrangementIngestionService;

    @Mock
    ArrangementRestMapper arrangementRestMapper;

    @Test
    void pullIngestArrangement_Success() {
        ArrangementPullIngestionRequest request = new ArrangementPullIngestionRequest();
        ArrangementIngestPullRequest pullRequest = ArrangementIngestPullRequest.builder().build();

        when(arrangementRestMapper.mapPullRequest(request)).thenReturn(pullRequest);

        ArrangementIngestResponse ingestResponse = ArrangementIngestResponse.builder().build();
        when(arrangementIngestionService.ingestPull(pullRequest)).thenReturn(Mono.just(ingestResponse));

        ResponseEntity<ArrangementIngestionResponse> responseEntity = mock(ResponseEntity.class);
        when(arrangementRestMapper.mapResponse(ingestResponse)).thenReturn(responseEntity);

        Mono<ResponseEntity<ArrangementIngestionResponse>> responseEntityMono =
            arrangementSubController.pullIngestArrangement(Mono.just(request), null);

        StepVerifier.create(responseEntityMono).expectNext(responseEntity).verifyComplete();
    }

    @Test
    void pushIngestArrangements_Success() {
        ArrangementPushIngestionRequest request = new ArrangementPushIngestionRequest();
        ArrangementIngestPushRequest pushRequest = ArrangementIngestPushRequest.builder().build();
        when(arrangementRestMapper.mapPushRequest(request)).thenReturn(pushRequest);

        ArrangementIngestResponse ingestResponse = ArrangementIngestResponse.builder().build();
        when(arrangementIngestionService.ingestPush(pushRequest)).thenReturn(Mono.just(ingestResponse));

        ResponseEntity<ArrangementIngestionResponse> responseEntity = mock(ResponseEntity.class);
        when(arrangementRestMapper.mapResponse(ingestResponse)).thenReturn(responseEntity);

        Mono<ResponseEntity<ArrangementIngestionResponse>> responseEntityMono =
            arrangementSubController.pushIngestArrangement(Mono.just(request), null);

        StepVerifier.create(responseEntityMono).expectNext(responseEntity).verifyComplete();
    }
}
