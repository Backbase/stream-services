package com.backbase.stream.compositions.product.http;

import com.backbase.stream.compositions.product.api.ProductCompositionApi;
import com.backbase.stream.compositions.product.api.model.ArrangementIngestionResponse;
import com.backbase.stream.compositions.product.api.model.ArrangementPullIngestionRequest;
import com.backbase.stream.compositions.product.api.model.ArrangementPushIngestionRequest;
import com.backbase.stream.compositions.product.api.model.ProductIngestionResponse;
import com.backbase.stream.compositions.product.api.model.ProductPullIngestionRequest;
import com.backbase.stream.compositions.product.api.model.ProductPushIngestionRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
@Slf4j
public class ProductController implements ProductCompositionApi {

  private ProductSubController productSubController;
  private ArrangementSubController arrangementSubController;

  @Override
  public Mono<ResponseEntity<ArrangementIngestionResponse>> pullIngestArrangement(
      Mono<ArrangementPullIngestionRequest> arrangementPullIngestionRequest,
      ServerWebExchange exchange) {
    return arrangementSubController.pullIngestArrangement(
        arrangementPullIngestionRequest, exchange);
  }

  @Override
  public Mono<ResponseEntity<ProductIngestionResponse>> pullIngestProduct(
      Mono<ProductPullIngestionRequest> productPullIngestionRequest, ServerWebExchange exchange) {
    return productSubController.pullIngestProduct(productPullIngestionRequest, exchange);
  }

  @Override
  public Mono<ResponseEntity<ArrangementIngestionResponse>> pushIngestArrangement(
      Mono<ArrangementPushIngestionRequest> arrangementPushIngestionRequest,
      ServerWebExchange exchange) {
    return arrangementSubController.pushIngestArrangement(
        arrangementPushIngestionRequest, exchange);
  }

  @Override
  public Mono<ResponseEntity<ProductIngestionResponse>> pushIngestProduct(
      Mono<ProductPushIngestionRequest> productPushIngestionRequest, ServerWebExchange exchange) {
    return productSubController.pushIngestProduct(productPushIngestionRequest, exchange);
  }
}
