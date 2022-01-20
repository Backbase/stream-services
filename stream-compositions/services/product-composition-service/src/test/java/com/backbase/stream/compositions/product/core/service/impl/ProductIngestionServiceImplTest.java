package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.stream.compositions.integration.product.model.ProductGroup;
import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import com.backbase.stream.compositions.product.core.service.ProductIntegrationService;
import com.backbase.stream.product.BatchProductIngestionSaga;
import com.backbase.stream.product.task.ProductGroupTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductIngestionServiceImplTest {
    private ProductIngestionService productIngestionService;

    @Mock
    private ProductIntegrationService productIntegrationService;

    @Mock
    ProductGroupMapper mapper;

    @Mock
    BatchProductIngestionSaga batchProductIngestionSaga;

    @BeforeEach
    void setUp() {
        productIngestionService = new ProductIngestionServiceImpl(
                mapper,
                batchProductIngestionSaga,
                productIntegrationService);
    }

    @Test
    void ingestionInPullMode_Success() {
        Mono<ProductIngestPullRequest> productIngestPullRequest = Mono.just(ProductIngestPullRequest.builder()
                .legalEntityExternalId("externalId")
                .build());
        ProductGroup productGroup = new ProductGroup();

        when(productIntegrationService.pullProductGroup(productIngestPullRequest.block()))
                .thenReturn(Mono.just(productGroup));

        when(mapper.mapIntegrationToStream(productGroup))
                .thenReturn(new com.backbase.stream.legalentity.model.ProductGroup());

        ProductGroupTask productGroupTask = new ProductGroupTask();
        productGroupTask.setProductGroup(new com.backbase.stream.legalentity.model.ProductGroup());

        when(batchProductIngestionSaga.process(any(ProductGroupTask.class)))
                .thenReturn(Mono.just(productGroupTask));

        ProductIngestResponse productIngestResponse = productIngestionService.ingestPull(productIngestPullRequest).block();
        assertNotNull(productIngestResponse);
        assertNotNull(productIngestResponse.getProductGroup());
    }

    @Test
    void ingestionInPushMode_Unsupported() {
        Mono<ProductIngestPushRequest> request = Mono.just(ProductIngestPushRequest.builder().build());
        assertThrows(UnsupportedOperationException.class, () -> {
            productIngestionService.ingestPush(request);
        });
    }
}
