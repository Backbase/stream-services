package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.stream.compositions.integration.product.api.ProductIntegrationApi;
import com.backbase.stream.compositions.integration.product.model.ProductGroup;
import com.backbase.stream.compositions.integration.product.model.PullProductGroupResponse;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductIntegrationServiceImplTest {
    @Mock
    private ProductIntegrationApi productIntegrationApi;

    private ProductIntegrationServiceImpl productIntegrationService;

    @BeforeEach
    void setUp() {
        productIntegrationService = new ProductIntegrationServiceImpl(productIntegrationApi);
    }

    @Test
    void callIntegrationService_Success() throws UnsupportedOperationException {
        ProductGroup productGroup = new ProductGroup();

        PullProductGroupResponse getProductGroupResponse = new PullProductGroupResponse().
                productGroup(productGroup);
        when(productIntegrationApi.pullProductGroup(any(),any()))
                .thenReturn(Mono.just(getProductGroupResponse));

        ProductIngestPullRequest request = ProductIngestPullRequest.builder().legalEntityExternalId("externalId").build();

        ProductGroup response = productIntegrationService.pullProductGroup(request).block();
        assertEquals(productGroup, response);
    }

    @Test
    void callIntegrationService_EmptyLegalEntityList() throws UnsupportedOperationException {
        when(productIntegrationApi.pullProductGroup(any(), any())).thenReturn(Mono.empty());

        ProductIngestPullRequest request = ProductIngestPullRequest.builder().legalEntityExternalId("externalId").build();
        ProductGroup response = productIntegrationService.pullProductGroup(request).block();
        assertEquals(null, response);
    }
}
