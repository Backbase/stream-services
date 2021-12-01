package com.backbase.stream.compositions.legalentity.http;

import com.backbase.stream.compositions.legalentity.api.LegalEntityCompositionApi;
import com.backbase.stream.compositions.legalentity.core.LegalEntityIngestionService;
import com.backbase.stream.compositions.legalentity.core.RequestSource;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<IngestionResponse> pullIngestLegalEntity(@Valid PullIngestionRequest pullIngestionRequest) {
        LegalEntityIngestResponse response = legalEntityIngestionService.ingestPull(buildRequest(pullIngestionRequest));
        return ResponseEntity.ok(buildResponse(response));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<IngestionResponse> pushIngestLegalEntity(@Valid PushIngestionRequest pushIngestionRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param pullIngestionRequest PullIngestionRequest
     * @return LegalEntityIngestPullRequest
     */
    private LegalEntityIngestPullRequest buildRequest(PullIngestionRequest pullIngestionRequest) {
        return LegalEntityIngestPullRequest.builder()
                .soure(RequestSource.HTTP)
                .legalEntityExternalId(pullIngestionRequest.getLegalEntityExternalId())
                .build();
    }

    /**
     * Builds ingestion response for API endpoint.
     *
     * @param response LegalEntityIngestResponse
     * @return IngestionResponse
     */
    private IngestionResponse buildResponse(LegalEntityIngestResponse response) {
        return new IngestionResponse()
                .withLegalEntities(response.getLegalEntities().stream()
                        .map(item -> mapper.mapStreamToComposition(item))
                        .collect(Collectors.toList()));
    }
}
