package com.backbase.stream.compositions.legalentity.http;

import com.backbase.stream.compositions.legalentity.api.LegalEntityCompositionApi;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPushRequest;
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
        LegalEntityIngestResponse response = legalEntityIngestionService.ingestPull(buildRequest(pullIngestionRequest)).block();
        return ResponseEntity.ok(buildResponse(response));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<IngestionResponse> pushIngestLegalEntity(@Valid PushIngestionRequest pushIngestionRequest) {
        LegalEntityIngestResponse response = legalEntityIngestionService.ingestPush(buildRequest(pushIngestionRequest)).block();
        return ResponseEntity.ok(buildResponse(response));
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PullIngestionRequest
     * @return LegalEntityIngestPullRequest
     */
    private LegalEntityIngestPullRequest buildRequest(PullIngestionRequest request) {
        return LegalEntityIngestPullRequest.builder()
                .legalEntityExternalId(request.getLegalEntityExternalId())
                .build();
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PushIngestionRequest
     * @return LegalEntityIngestPushRequest
     */
    private LegalEntityIngestPushRequest buildRequest(PushIngestionRequest request) {
        return LegalEntityIngestPushRequest.builder()
                .legalEntities(request.getLegalEntities()
                        .stream()
                        .map(mapper::mapCompostionToStream).collect(Collectors.toList()))
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
                .withLegalEntities(response.getLegalEntities()
                        .stream()
                        .map(mapper::mapStreamToComposition)
                        .collect(Collectors.toList()));
    }
}
