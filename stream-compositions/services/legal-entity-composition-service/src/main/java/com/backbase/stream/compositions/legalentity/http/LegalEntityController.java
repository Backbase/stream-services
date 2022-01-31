package com.backbase.stream.compositions.legalentity.http;

import com.backbase.stream.compositions.legalentity.api.LegalEntityCompositionApi;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestPushRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityIngestResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import com.backbase.stream.compositions.legalentity.model.LegalEntityIngestionResponse;
import com.backbase.stream.compositions.legalentity.model.LegalEntityPullIngestionRequest;
import com.backbase.stream.compositions.legalentity.model.LegalEntityPushIngestionRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@AllArgsConstructor
public class LegalEntityController implements LegalEntityCompositionApi {
    private final LegalEntityIngestionService legalEntityIngestionService;
    private final LegalEntityMapper mapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<LegalEntityIngestionResponse>> pullIngestLegalEntity(
            @Valid Mono<LegalEntityPullIngestionRequest> pullIngestionRequest, ServerWebExchange exchange) {
        return legalEntityIngestionService
                .ingestPull(pullIngestionRequest.map(this::buildPullRequest))
                .map(this::mapIngestionToResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<LegalEntityIngestionResponse>> pushIngestLegalEntity(
            @Valid Mono<LegalEntityPushIngestionRequest> pushIngestionRequest, ServerWebExchange exchange) {
        return legalEntityIngestionService
                .ingestPush(pushIngestionRequest.map(this::buildPushRequest))
                .map(this::mapIngestionToResponse);
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PullIngestionRequest
     * @return LegalEntityIngestPullRequest
     */
    private LegalEntityIngestPullRequest buildPullRequest(LegalEntityPullIngestionRequest request) {
        return LegalEntityIngestPullRequest
                .builder()
                .legalEntityExternalId(request.getLegalEntityExternalId())
                .additionalParameters(request.getAdditionalParameters())
                .build();
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PushIngestionRequest
     * @return LegalEntityIngestPushRequest
     */
    private LegalEntityIngestPushRequest buildPushRequest(LegalEntityPushIngestionRequest request) {
        return LegalEntityIngestPushRequest
                .builder()
                .legalEntity(mapper.mapCompostionToStream(request.getLegalEntity()))
                .build();
    }

    /**
     * Builds ingestion response for API endpoint.
     *
     * @param response LegalEntityIngestResponse
     * @return ResponseEntity<IngestionResponse>
     */
    private ResponseEntity<LegalEntityIngestionResponse> mapIngestionToResponse(LegalEntityIngestResponse response) {
        return new ResponseEntity<>(
                new LegalEntityIngestionResponse()
                        .withLegalEntity(mapper.mapStreamToComposition(response.getLegalEntity())),
                HttpStatus.CREATED);
    }
}
