package com.backbase.stream.compositions.productcatalog.http;

import com.backbase.stream.compositions.productcatalog.core.model.ProductCatalogIngestResponse;
import com.backbase.stream.compositions.productcatalog.core.service.ProductCatalogIngestionService;
import com.backbase.stream.compositions.productcatalog.mapper.ProductCatalogMapper;
import com.backbase.stream.compositions.productcatalog.model.ProductCatalogIngestionResponse;
import com.backbase.stream.compositions.productcatalog.model.ProductCatalog;
import com.backbase.stream.compositions.productcatalog.model.ProductCatalogPullIngestionRequest;
import com.backbase.stream.compositions.productcatalog.model.ProductCatalogPushIngestionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCatalogControllerTest {
    @Mock
    ProductCatalogMapper mapper;

    @Mock
    ProductCatalogIngestionService productCatalogIngestionService;

    ProductCatalogController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductCatalogController(
                productCatalogIngestionService,
                mapper);

        lenient().when(mapper.mapCompositionToStream(any())).thenReturn(new com.backbase.stream.productcatalog.model.ProductCatalog());
        lenient().when(mapper.mapStreamToComposition(any())).thenReturn(new ProductCatalog());
    }

    @Test
    void testPullIngestion_Success() {
        when(productCatalogIngestionService.ingestPull(any())).thenReturn(
                Mono.just(ProductCatalogIngestResponse.builder()
                        .build()));

        ProductCatalogPullIngestionRequest request = new ProductCatalogPullIngestionRequest();
        ResponseEntity<ProductCatalogIngestionResponse> responseEntity = controller.pullIngestProductCatalog(Mono.just(request), null).block();
        ProductCatalogIngestionResponse ingestionResponse = responseEntity.getBody();
        assertNotNull(ingestionResponse);
        assertNotNull(ingestionResponse.getProductCatalog());
        verify(productCatalogIngestionService).ingestPull(any());
    }

    @Test
    void testPushIngestion_Success() {
        Mono<ProductCatalogPushIngestionRequest> requestMono = Mono.just(
                new ProductCatalogPushIngestionRequest().productCatalog(new ProductCatalog()));

        doAnswer(invocation -> {
            Mono mono = invocation.getArgument(0);
            mono.block();

            return Mono.just(ProductCatalogIngestResponse.builder()
                    .productCatalog(
                            new com.backbase.stream.productcatalog.model.ProductCatalog()
                                    .productKinds(new ArrayList<>())
                                    .productTypes(new ArrayList<>()))
                    .build());
        }).when(productCatalogIngestionService).ingestPush(any());

        ResponseEntity<ProductCatalogIngestionResponse> responseEntity = controller.pushIngestProductCatalog(requestMono, null).block();
        ProductCatalogIngestionResponse ingestionResponse = responseEntity.getBody();
        assertNotNull(ingestionResponse);
        assertNotNull(ingestionResponse.getProductCatalog());
        verify(productCatalogIngestionService).ingestPush(any());
    }
}
