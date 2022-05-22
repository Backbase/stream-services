package com.backbase.stream.compositions.legalentity.core.service;

import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;

public interface LegalEntityPostIngestionService {
    /**
     * Post processing for a completed ingestion process
     * @param response
     * @param errors
     */
    public void handleSuccess(LegalEntityResponse response);

    /**
     * Post processing for a failed ingestion process
     * @param response
     */
    public void handleFailure(LegalEntityResponse response);
}
