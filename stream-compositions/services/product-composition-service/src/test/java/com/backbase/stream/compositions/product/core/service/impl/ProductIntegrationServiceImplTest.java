package com.backbase.stream.compositions.product.core.service.impl;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.integration.product.api.ProductIntegrationApi;
import com.backbase.stream.compositions.integration.product.model.ProductGroup;
import com.backbase.stream.compositions.integration.product.model.PullProductGroupResponse;
import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapperImpl;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ProductIntegrationServiceImplTest {

    @Mock
    private ProductIntegrationApi productIntegrationApi;

    private final ProductGroupMapper productGroupMapper = new ProductGroupMapperImpl();

    private ProductIntegrationServiceImpl productIntegrationService;

    @BeforeEach
    void setUp() {
        productIntegrationService = new ProductIntegrationServiceImpl(productIntegrationApi,
                productGroupMapper);
    }

    @Test
    void callIntegrationService_Success() throws UnsupportedOperationException {
        PullProductGroupResponse getProductGroupResponse = new PullProductGroupResponse().
                productGroups(singletonList(new ProductGroup()));
        when(productIntegrationApi.pullProductGroup(any()))
                .thenReturn(Mono.just(getProductGroupResponse));
        Map<String, String> additions = Map.of("addition", "addition1");


        ProductIngestPullRequest request = ProductIngestPullRequest.builder()
                .serviceAgreementExternalId("sa_externalId")
                .serviceAgreementInternalId("sa_internalId")
                .legalEntityExternalId("sa_externalId")
                .legalEntityInternalId("le_internalId")
                .userExternalId("user_externalId")
                .userInternalId("user_internalId")
                .source("source_of_ingestion_process")
                .additions(additions)
                .build();

        com.backbase.stream.legalentity.model.ProductGroup productGroup1 = new com.backbase.stream.legalentity.model.ProductGroup();
        ProductIngestResponse expectedResponse = new ProductIngestResponse("id1", "id2",
                singletonList(productGroup1), additions);
        expectedResponse.setServiceAgreementInternalId("sa_internalId");
        expectedResponse.setServiceAgreementExternalId("sa_externalId");
        expectedResponse.setLegalEntityInternalId("le_internalId");
        expectedResponse.setLegalEntityExternalId("sa_externalId");
        expectedResponse.setUserInternalId("user_internalId");
        expectedResponse.setUserExternalId("user_externalId");
        expectedResponse.setSource("source_of_ingestion_process");
        StepVerifier.create(productIntegrationService.pullProductGroup(request))
                .expectNextMatches(response -> response.getLegalEntityInternalId().equals(expectedResponse.getLegalEntityInternalId()) &&
                        response.getLegalEntityExternalId().equals(expectedResponse.getLegalEntityExternalId()) &&
                        response.getServiceAgreementInternalId().equals(expectedResponse.getServiceAgreementInternalId()) &&
                        response.getServiceAgreementExternalId().equals(expectedResponse.getServiceAgreementExternalId()) &&
                        response.getUserInternalId().equals(expectedResponse.getUserInternalId()) &&
                        response.getUserExternalId().equals(expectedResponse.getUserExternalId()) &&
                        response.getSource().equals(expectedResponse.getSource())
                        )
                .verifyComplete();
    }

    @Test
    void callIntegrationService_Failure() throws UnsupportedOperationException {
        when(productIntegrationApi.pullProductGroup(any()))
                .thenReturn(Mono.error(new RuntimeException("error")));

        ProductIngestPullRequest request = ProductIngestPullRequest.builder()
                .legalEntityExternalId("externalId").build();
        StepVerifier.create(productIntegrationService.pullProductGroup(request)).expectError().verify();
    }
}
