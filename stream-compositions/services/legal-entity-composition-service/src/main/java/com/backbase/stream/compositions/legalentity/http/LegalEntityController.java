package com.backbase.stream.compositions.legalentity.http;

import com.backbase.stream.compositions.legalentity.api.LegalEntityCompositionApi;
import com.backbase.stream.compositions.legalentity.model.IngestionResponse;
import com.backbase.stream.compositions.legalentity.model.PullIngestionRequest;
import com.backbase.stream.compositions.legalentity.model.PushIngestionRequest;
import org.springframework.http.ResponseEntity;

import javax.validation.Valid;

public class LegalEntityController implements LegalEntityCompositionApi {
    @Override
    public ResponseEntity<IngestionResponse> pullIngestLegalEntity(@Valid PullIngestionRequest pullIngestionRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResponseEntity<IngestionResponse> pushIngestLegalEntity(@Valid PushIngestionRequest pushIngestionRequest) {
        throw new UnsupportedOperationException();
    }
}
