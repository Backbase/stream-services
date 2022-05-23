package com.backbase.stream.compositions.legalentity.core.service.impl;

import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
import com.backbase.stream.compositions.legalentity.integration.client.LegalEntityIntegrationApi;
import com.backbase.stream.compositions.legalentity.integration.client.model.LegalEntity;
import com.backbase.stream.compositions.legalentity.integration.client.model.PullLegalEntityResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalEntityIntegrationServiceImplTest {
    @Mock
    private LegalEntityIntegrationApi legalEntityIntegrationApi;

    @Mock
    private LegalEntityMapper legalEntityMapper;

    private LegalEntityIntegrationServiceImpl legalEntityIntegrationService;

    @BeforeEach
    void setUp() {
        legalEntityIntegrationService = new LegalEntityIntegrationServiceImpl(
                legalEntityIntegrationApi, legalEntityMapper);
    }

    @Test
    void callLE() throws UnsupportedOperationException {
        String leName = "Legal Entity 1";
        LegalEntity legalEntity1 = new LegalEntity().withName(leName);
        com.backbase.stream.legalentity.model.LegalEntity legalEntity2 =
                new com.backbase.stream.legalentity.model.LegalEntity().name(leName);
        List<String> membershipAccounts = Collections.singletonList("012");
        LegalEntityResponse res1 = new LegalEntityResponse(legalEntity2, membershipAccounts);
        Mono<PullLegalEntityResponse> res = Mono.just(new PullLegalEntityResponse()
                .withLegalEntity(legalEntity1).withMembershipAccounts(Collections.singletonList("012")));

        when(legalEntityIntegrationApi.pullLegalEntity(any())).thenReturn(res);
        when(legalEntityMapper.mapResponseIntegrationToStream(any())).thenReturn(res1);

        StepVerifier.create(legalEntityIntegrationService.pullLegalEntity(new LegalEntityPullRequest()))
                .expectNext(res1)
                .expectComplete()
                .verify();
    }


    @Test
    void callIntegrationService_LegalEntitiesFound() throws UnsupportedOperationException {
        LegalEntity legalEntity1 = new LegalEntity().withName("Legal Entity 1");
        com.backbase.stream.legalentity.model.LegalEntity legalEntity2 =
                new com.backbase.stream.legalentity.model.LegalEntity().name("Legal Entity 1");
        List<String> membershipAccounts = Collections.singletonList("012");
        LegalEntityResponse res1 = new LegalEntityResponse(legalEntity2, membershipAccounts);
        PullLegalEntityResponse getLegalEntityListResponse = new PullLegalEntityResponse().withLegalEntity(legalEntity1);

        when(legalEntityIntegrationApi.pullLegalEntity(any()))
                .thenReturn(Mono.just(getLegalEntityListResponse));
        when(legalEntityMapper.mapResponseIntegrationToStream(any())).thenReturn(res1);

        LegalEntityResponse legalEntityResponse = legalEntityIntegrationService.pullLegalEntity(
                LegalEntityPullRequest.builder().legalEntityExternalId("externalId").build()).block();

        assertEquals("Legal Entity 1", legalEntityResponse.getLegalEntity().getName());
    }


    @Test
    void callIntegrationService_EmptyLegalEntityList() throws UnsupportedOperationException {
        when(legalEntityIntegrationApi.pullLegalEntity(any()))
                .thenReturn(Mono.empty());

        LegalEntityResponse legalEntityResponse = legalEntityIntegrationService.pullLegalEntity(
                LegalEntityPullRequest.builder().legalEntityExternalId("externalId").build()).block();

        assertNull(legalEntityResponse);
    }
}

