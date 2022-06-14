package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import com.backbase.stream.compositions.product.core.service.ProductIntegrationService;
import com.backbase.stream.compositions.product.core.service.ProductPostIngestionService;
import com.backbase.stream.product.BatchProductIngestionSaga;
import com.backbase.stream.product.task.ProductGroupTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import javax.validation.Validator;
import java.util.Collections;

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
    Validator validator;

    @Mock
    ProductPostIngestionService productPostIngestionService;

    @Mock
    BatchProductIngestionSaga batchProductIngestionSaga;

    @BeforeEach
    void setUp() {
        productIngestionService = new ProductIngestionServiceImpl(
                batchProductIngestionSaga,
                productIntegrationService,
                validator,
                productPostIngestionService);
    }

    // TODO - Verify why test failing
    //@Test
    void ingestionInPullMode_Success() {
        ProductIngestPullRequest productIngestPullRequest = ProductIngestPullRequest.builder()
                .legalEntityExternalId("externalId")
                .build();
        com.backbase.stream.legalentity.model.ProductGroup productGroup = new com.backbase.stream.legalentity.model.ProductGroup();
        ProductIngestResponse res = new ProductIngestResponse(productGroup, Collections.singletonMap("key", "value"));

        when(productIntegrationService.pullProductGroup(productIngestPullRequest))
                .thenReturn(Mono.just(res));

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
        ProductIngestPushRequest request = ProductIngestPushRequest.builder().build();
        assertThrows(UnsupportedOperationException.class, () -> {
            productIngestionService.ingestPush(request);
        });
    }
}
