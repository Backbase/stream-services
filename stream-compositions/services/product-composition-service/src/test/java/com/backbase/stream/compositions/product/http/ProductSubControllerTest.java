package com.backbase.stream.compositions.product.http;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backbase.stream.compositions.product.api.model.ProductIngestionResponse;
import com.backbase.stream.compositions.product.api.model.ProductPullIngestionRequest;
import com.backbase.stream.compositions.product.api.model.ProductPushIngestionRequest;
import com.backbase.stream.compositions.product.core.mapper.ProductRestMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ProductSubControllerTest {
    @InjectMocks ProductSubController productSubController;

    @Mock ProductIngestionService productIngestionService;

    @Mock ProductRestMapper productRestMapper;

    @Test
    void pullIngestProduct_Success() {
        ProductPullIngestionRequest request = new ProductPullIngestionRequest();
        ProductIngestPullRequest pullRequest = ProductIngestPullRequest.builder().build();

        when(productRestMapper.mapPullRequest(request)).thenReturn(pullRequest);

        ProductIngestResponse ingestResponse = ProductIngestResponse.builder().build();
        when(productIngestionService.ingestPull(pullRequest)).thenReturn(Mono.just(ingestResponse));

        ResponseEntity<ProductIngestionResponse> responseEntity = mock(ResponseEntity.class);
        when(productRestMapper.mapResponse(ingestResponse)).thenReturn(responseEntity);

        Mono<ResponseEntity<ProductIngestionResponse>> responseEntityMono =
                productSubController.pullIngestProduct(Mono.just(request), null);

        StepVerifier.create(responseEntityMono).expectNext(responseEntity).verifyComplete();
    }

    @Test
    void pushIngestProducts_Success() {
        ProductPushIngestionRequest request = new ProductPushIngestionRequest();
        ProductIngestPushRequest pushRequest = ProductIngestPushRequest.builder().build();
        when(productRestMapper.mapPushRequest(request)).thenReturn(pushRequest);

        ProductIngestResponse ingestResponse = ProductIngestResponse.builder().build();
        when(productIngestionService.ingestPush(pushRequest)).thenReturn(Mono.just(ingestResponse));

        ResponseEntity<ProductIngestionResponse> responseEntity = mock(ResponseEntity.class);
        when(productRestMapper.mapResponse(ingestResponse)).thenReturn(responseEntity);

        Mono<ResponseEntity<ProductIngestionResponse>> responseEntityMono =
                productSubController.pushIngestProduct(Mono.just(request), null);

        StepVerifier.create(responseEntityMono).expectNext(responseEntity).verifyComplete();
    }
}
