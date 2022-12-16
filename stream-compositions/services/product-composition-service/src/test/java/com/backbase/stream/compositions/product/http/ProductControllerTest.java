package com.backbase.stream.compositions.product.http;

import com.backbase.stream.compositions.product.api.model.ProductGroup;
import com.backbase.stream.compositions.product.api.model.ProductIngestionResponse;
import com.backbase.stream.compositions.product.api.model.ProductPullIngestionRequest;
import com.backbase.stream.compositions.product.api.model.ProductPushIngestionRequest;
import com.backbase.stream.compositions.product.core.mapper.ArrangementMapper;
import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Disabled
@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    ProductGroupMapper mapper;

    @Mock
    ArrangementMapper arrangementMapper;

    @Mock
    ProductSubController productSubController;

    @Mock
    ArrangementSubController arrangementSubController;

    ProductController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductController(
                productSubController,
                arrangementSubController);

        lenient().when(mapper.mapCompositionToStream(any()))
                .thenReturn(new com.backbase.stream.legalentity.model.ProductGroup());
        lenient().when(mapper.mapStreamToComposition(any())).thenReturn(new ProductGroup());
    }

    @Test
    void testPullIngestion_Success() {
        Mono<ProductPullIngestionRequest> requestMono = Mono.just(
                new ProductPullIngestionRequest().withLegalEntityExternalId("externalId"));

        doAnswer(invocation -> {
            ProductIngestPullRequest request = invocation.getArgument(0);

            return Mono.just(ProductIngestResponse.builder()
                    .productGroups(Arrays.asList(new com.backbase.stream.legalentity.model.ProductGroup()))
                    .build());
        }).when(productSubController).pullIngestProduct(any(), any());

        ResponseEntity<ProductIngestionResponse> responseEntity = controller
                .pullIngestProduct(requestMono, null).block();
        ProductIngestionResponse ingestionResponse = responseEntity.getBody();
        assertNotNull(ingestionResponse);
        assertNotNull(ingestionResponse.getProductGroups());
        verify(productSubController).pullIngestProduct(any(), any());
    }

    @Test
    void testPushIngestion_Success() {
        Mono<ProductPushIngestionRequest> requestMono = Mono.just(
                new ProductPushIngestionRequest().withProductGroup(new ProductGroup()));

        doAnswer(invocation -> {
            ProductIngestPushRequest request = invocation.getArgument(0);

            return Mono.just(ProductIngestResponse.builder()
                    .productGroups(Arrays.asList(new com.backbase.stream.legalentity.model.ProductGroup()))
                    .build());
        }).when(productSubController).pushIngestProduct(any(), any());

        ResponseEntity<ProductIngestionResponse> responseEntity = controller
                .pushIngestProduct(requestMono, null).block();
        ProductIngestionResponse ingestionResponse = responseEntity.getBody();
        assertNotNull(ingestionResponse);
        assertNotNull(ingestionResponse.getProductGroups());
        verify(productSubController).pushIngestProduct(any(), any());
    }
}
