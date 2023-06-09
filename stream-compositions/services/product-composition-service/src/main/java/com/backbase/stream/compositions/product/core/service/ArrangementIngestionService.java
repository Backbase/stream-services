package com.backbase.stream.compositions.product.core.service;

import com.backbase.stream.compositions.product.core.model.ArrangementIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestResponse;
import reactor.core.publisher.Mono;

public interface ArrangementIngestionService {

  /**
   * Ingests arrangement in pull mode.
   *
   * @param ingestionRequest Ingest pull request
   * @return ArrangementIngestionResponse
   */
  Mono<ArrangementIngestResponse> ingestPull(ArrangementIngestPullRequest ingestionRequest);

  /**
   * Ingests arrangement in push mode.
   *
   * @param ingestPushRequest Ingest push request
   * @return ProductIngestResponse
   */
  Mono<ArrangementIngestResponse> ingestPush(ArrangementIngestPushRequest ingestPushRequest);
}
