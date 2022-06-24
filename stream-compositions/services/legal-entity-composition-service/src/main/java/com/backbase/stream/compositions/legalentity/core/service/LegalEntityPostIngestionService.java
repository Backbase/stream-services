package com.backbase.stream.compositions.legalentity.core.service;

import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
import reactor.core.publisher.Mono;

public interface LegalEntityPostIngestionService {

  /**
   * Post processing for a completed ingestion process
   *
   * @param response
   */
  Mono<LegalEntityResponse> handleSuccess(LegalEntityResponse response);

  /**
   * Post processing for a failed ingestion process
   *
   * @param error
   */
  void handleFailure(Throwable error);
}