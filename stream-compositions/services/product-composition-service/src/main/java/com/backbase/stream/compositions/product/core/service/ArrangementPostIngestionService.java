package com.backbase.stream.compositions.product.core.service;

import com.backbase.stream.compositions.product.core.model.ArrangementIngestResponse;
import reactor.core.publisher.Mono;

public interface ArrangementPostIngestionService {
  /**
   * Post-processing for a completed ingestion process
   *
   * @param response
   */
  Mono<ArrangementIngestResponse> handleSuccess(ArrangementIngestResponse response);

  /**
   * Post-processing for a failed ingestion process
   *
   * @param error
   */
  void handleFailure(Throwable error);
}
