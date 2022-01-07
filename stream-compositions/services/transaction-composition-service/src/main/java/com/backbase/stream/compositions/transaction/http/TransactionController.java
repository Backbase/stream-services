package com.backbase.stream.compositions.transaction.http;

import com.backbase.stream.compositions.transaction.api.TransactionCompositionApi;
import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPullRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPushRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestResponse;
import com.backbase.stream.compositions.transaction.core.service.TransactionIngestionService;
import com.backbase.stream.compositions.transaction.model.IngestionResponse;
import com.backbase.stream.compositions.transaction.model.PullIngestionRequest;
import com.backbase.stream.compositions.transaction.model.PushIngestionRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
public class TransactionController implements TransactionCompositionApi {
    private final TransactionIngestionService transactionIngestionService;
    private final TransactionMapper mapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<IngestionResponse>> pullIngestTransactions(
            @Valid Mono<PullIngestionRequest> pullIngestionRequest, ServerWebExchange exchange) {
        return transactionIngestionService
                .ingestPull(pullIngestionRequest.map(this::buildPullRequest))
                .map(this::mapIngestionToResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<IngestionResponse>> pushIngestTransactions(
            @Valid Mono<PushIngestionRequest> pushIngestionRequest, ServerWebExchange exchange) {
        return transactionIngestionService
                .ingestPush(pushIngestionRequest.map(this::buildPushRequest))
                .map(this::mapIngestionToResponse);
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PullIngestionRequest
     * @return ProductIngestPullRequest
     */
    private TransactionIngestPullRequest buildPullRequest(PullIngestionRequest request) {
        return TransactionIngestPullRequest
                .builder()
                .externalArrangementIds(request.getExternalArrangementIds())
                .additionalParameters(request.getAdditionalParameters())
                .build();
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PushIngestionRequest
     * @return ProductIngestPushRequest
     */
    private TransactionIngestPushRequest buildPushRequest(PushIngestionRequest request) {
        return TransactionIngestPushRequest.builder()
                .transactions(
                        request.getTransactions().stream().map(item -> mapper.mapCompositionToStream(item)).collect(Collectors.toList()))
                .build();
    }

    /**
     * Builds ingestion response for API endpoint.
     *
     * @param response ProductCatalogIngestResponse
     * @return IngestionResponse
     */
    private ResponseEntity<IngestionResponse> mapIngestionToResponse(TransactionIngestResponse response) {
        return new ResponseEntity<>(
                new IngestionResponse()
                        .withTransactions(
                                response.getTransactions().stream().map(item -> mapper.mapStreamToComposition(item)).collect(Collectors.toList())),
                HttpStatus.CREATED);
    }
}
