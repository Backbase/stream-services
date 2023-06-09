package com.backbase.stream.compositions.product.core.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.integration.product.api.ArrangementIntegrationApi;
import com.backbase.stream.compositions.integration.product.model.AccountArrangementItemPut;
import com.backbase.stream.compositions.integration.product.model.PullArrangementResponse;
import com.backbase.stream.compositions.product.core.mapper.ArrangementMapper;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ArrangementIntegrationServiceImplTest {

    @InjectMocks
    ArrangementIntegrationServiceImpl arrangementIntegrationService;

    @Mock
    ArrangementIntegrationApi arrangementIntegrationApi;

    @Mock
    ArrangementMapper arrangementMapper;

    @Test
    void pullArrangement_Success() {
        ArrangementIngestPullRequest request =
            ArrangementIngestPullRequest.builder()
                .arrangementId("arrangementId")
                .externalArrangementId("externalArrangementID")
                .build();

        when(arrangementIntegrationApi.pullArrangement(any()))
            .thenReturn(
                Mono.just(
                    new PullArrangementResponse()
                        .arrangement(
                            new AccountArrangementItemPut().productId("productId").name("name"))));
        when(arrangementMapper.mapIntegrationToStream(any()))
            .thenReturn(
                new com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPut());

        Mono<ArrangementIngestResponse> responseMono =
            arrangementIntegrationService.pullArrangement(request);

        StepVerifier.create(responseMono)
            .expectNextMatches(
                item -> {
                    return item != null;
                })
            .verifyComplete();
    }

    @Test
    void pullArrangement_Error() {
        ArrangementIngestPullRequest request =
            ArrangementIngestPullRequest.builder()
                .arrangementId("arrangementId")
                .externalArrangementId("externalArrangementID")
                .build();

        when(arrangementIntegrationApi.pullArrangement(any()))
            .thenReturn(Mono.error(new RuntimeException()));
        Mono<ArrangementIngestResponse> responseMono =
            arrangementIntegrationService.pullArrangement(request);

        StepVerifier.create(responseMono).verifyError();
    }
}
