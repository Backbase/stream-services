package com.backbase.stream.compositions.legalentity.http;

import com.backbase.stream.compositions.legalentity.api.LegalEntityCompositionApi;
import com.backbase.stream.compositions.legalentity.core.LegalEntityIngestionService;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestResponse;
import com.backbase.stream.compositions.legalentity.model.IngestionResponse;
import com.backbase.stream.compositions.legalentity.model.PullIngestionRequest;
import com.backbase.stream.compositions.legalentity.model.PushIngestionRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
public class LegalEntityController implements LegalEntityCompositionApi {
    private final LegalEntityIngestionService legalEntityIngestionService;
    private final LegalEntityMapper mapper;

    @Override
    public ResponseEntity<IngestionResponse> pullIngestLegalEntity(@Valid PullIngestionRequest pullIngestionRequest) {

        LegalEntityIngestResponse response = legalEntityIngestionService.ingestPull(
                LegalEntityIngestPullRequest.builder()
                        .legalEntityExternalId(pullIngestionRequest.getLegalEntityExternalId())
                        .build());

        return ResponseEntity.ok(ingestionResponse(response));
    }

    @Override
    public ResponseEntity<IngestionResponse> pushIngestLegalEntity(@Valid PushIngestionRequest pushIngestionRequest) {
        throw new UnsupportedOperationException();
    }

    private IngestionResponse ingestionResponse(LegalEntityIngestResponse response) {
        return new IngestionResponse()
                .withLegalEntities(response.getLegalEntities().stream()
                        .map(item -> mapper.mapStreamToComposition(item))
                        .collect(Collectors.toList()));
    }
}
