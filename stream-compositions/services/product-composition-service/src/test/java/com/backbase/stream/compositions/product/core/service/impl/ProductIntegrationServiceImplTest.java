package com.backbase.stream.compositions.product.core.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.integration.product.api.ProductIntegrationApi;
import com.backbase.stream.compositions.integration.product.model.ProductGroup;
import com.backbase.stream.compositions.integration.product.model.PullProductGroupResponse;
import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
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

  @Mock
  private ProductGroupMapper productGroupMapper;

  private ProductIntegrationServiceImpl productIntegrationService;

  @BeforeEach
  void setUp() {
    productIntegrationService = new ProductIntegrationServiceImpl(productIntegrationApi,
        productGroupMapper);
  }

  @Test
  void callIntegrationService_Success() throws UnsupportedOperationException {
    ProductGroup productGroup = new ProductGroup();
    com.backbase.stream.legalentity.model.ProductGroup productGroup1 = new com.backbase.stream.legalentity.model.ProductGroup();
    ProductIngestResponse res = new ProductIngestResponse(productGroup1, Map.of());

    PullProductGroupResponse getProductGroupResponse = new PullProductGroupResponse().
        productGroup(productGroup);
    when(productIntegrationApi.pullProductGroup(any()))
        .thenReturn(Mono.just(getProductGroupResponse));

    ProductIngestPullRequest request = ProductIngestPullRequest.builder()
        .legalEntityExternalId("externalId").build();

    StepVerifier.create(productIntegrationService.pullProductGroup(request))
        .expectNext(res)
        .expectComplete();
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
