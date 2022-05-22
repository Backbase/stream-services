package com.backbase.stream.compositions.product.http;

import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import com.backbase.stream.compositions.product.api.model.ProductIngestionResponse;
import com.backbase.stream.compositions.product.api.model.ProductGroup;
import com.backbase.stream.compositions.product.api.model.ProductPullIngestionRequest;
import com.backbase.stream.compositions.product.api.model.ProductPushIngestionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {
    @Mock
    ProductGroupMapper mapper;

    @Mock
    ProductIngestionService productIngestionService;

    ProductController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductController(
                productIngestionService,
                mapper);

        lenient().when(mapper.mapCompositionToStream(any())).thenReturn(new com.backbase.stream.legalentity.model.ProductGroup());
        lenient().when(mapper.mapStreamToComposition(any())).thenReturn(new ProductGroup());
    }

    @Test
    void testPullIngestion_Success() {
        Mono<ProductPullIngestionRequest> requestMono = Mono.just(
                new ProductPullIngestionRequest().withLegalEntityExternalId("externalId"));

        doAnswer(invocation -> {
            ProductIngestPullRequest request = invocation.getArgument(0);

            return Mono.just(ProductIngestResponse.builder()
                    .productGroup(
                            new com.backbase.stream.legalentity.model.ProductGroup())
                    .build());
        }).when(productIngestionService).ingestPull(any());

        ResponseEntity<ProductIngestionResponse> responseEntity = controller.pullIngestProduct(requestMono, null).block();
        ProductIngestionResponse ingestionResponse = responseEntity.getBody();
        assertNotNull(ingestionResponse);
        assertNotNull(ingestionResponse.getProductGgroup());
        verify(productIngestionService).ingestPull(any());
    }

    @Test
    void testPushIngestion_Success() {
        Mono<ProductPushIngestionRequest> requestMono = Mono.just(
                new ProductPushIngestionRequest().withProductGgroup(new ProductGroup()));

        doAnswer(invocation -> {
            ProductIngestPushRequest request = invocation.getArgument(0);

            return Mono.just(ProductIngestResponse.builder()
                    .productGroup(
                            new com.backbase.stream.legalentity.model.ProductGroup())
                    .build());
        }).when(productIngestionService).ingestPush(any());

        ResponseEntity<ProductIngestionResponse> responseEntity = controller.pushIngestProduct(requestMono, null).block();
        ProductIngestionResponse ingestionResponse = responseEntity.getBody();
        assertNotNull(ingestionResponse);
        assertNotNull(ingestionResponse.getProductGgroup());
        verify(productIngestionService).ingestPush(any());
    }
}

