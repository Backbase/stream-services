package com.backbase.stream.compositions.product.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.product.api.model.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {
    @InjectMocks ProductController controller;

    @Mock ProductSubController productSubController;

    @Mock ArrangementSubController arrangementSubController;

    @Test
    void pullProductIngestion_Success() {
        ProductPullIngestionRequest request = new ProductPullIngestionRequest();
        ResponseEntity<ProductIngestionResponse> responseEntity = mock(ResponseEntity.class);
        when(productSubController.pullIngestProduct(any(), any()))
                .thenReturn(Mono.just(responseEntity));

        Mono<ResponseEntity<ProductIngestionResponse>> responseEntityMono =
                controller.pullIngestProduct(Mono.just(request), null);

        StepVerifier.create(responseEntityMono).expectNext(responseEntity).verifyComplete();
    }

    @Test
    void pushProductIngestion_Success() {
        ProductPushIngestionRequest request = new ProductPushIngestionRequest();
        ResponseEntity<ProductIngestionResponse> responseEntity = mock(ResponseEntity.class);
        when(productSubController.pushIngestProduct(any(), any()))
                .thenReturn(Mono.just(responseEntity));

        Mono<ResponseEntity<ProductIngestionResponse>> responseEntityMono =
                controller.pushIngestProduct(Mono.just(request), null);

        StepVerifier.create(responseEntityMono).expectNext(responseEntity).verifyComplete();
    }

    @Test
    void pullArrangementIngestion_Success() {
        ArrangementPullIngestionRequest request = new ArrangementPullIngestionRequest();
        ResponseEntity<ArrangementIngestionResponse> responseEntity = mock(ResponseEntity.class);
        when(arrangementSubController.pullIngestArrangement(any(), any()))
                .thenReturn(Mono.just(responseEntity));

        Mono<ResponseEntity<ArrangementIngestionResponse>> responseEntityMono =
                controller.pullIngestArrangement(Mono.just(request), null);

        StepVerifier.create(responseEntityMono).expectNext(responseEntity).verifyComplete();
    }

    @Test
    void pushArrangementIngestion_Success() {
        ArrangementPushIngestionRequest request = new ArrangementPushIngestionRequest();
        ResponseEntity<ArrangementIngestionResponse> responseEntity = mock(ResponseEntity.class);
        when(arrangementSubController.pushIngestArrangement(any(), any()))
                .thenReturn(Mono.just(responseEntity));

        Mono<ResponseEntity<ArrangementIngestionResponse>> responseEntityMono =
                controller.pushIngestArrangement(Mono.just(request), null);

        StepVerifier.create(responseEntityMono).expectNext(responseEntity).verifyComplete();
    }
}
