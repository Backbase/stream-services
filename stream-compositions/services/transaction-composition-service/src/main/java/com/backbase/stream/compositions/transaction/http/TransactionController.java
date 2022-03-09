package com.backbase.stream.compositions.transaction.http;

import com.backbase.stream.compositions.integration.transaction.model.PullIngestionRequest;
import com.backbase.stream.compositions.transaction.api.TransactionCompositionApi;
import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPullRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPushRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestResponse;
import com.backbase.stream.compositions.transaction.core.service.TransactionIngestionService;
import com.backbase.stream.compositions.transaction.model.TransactionIngestionResponse;
import com.backbase.stream.compositions.transaction.model.TransactionPullIngestionRequest;
import com.backbase.stream.compositions.transaction.model.TransactionPushIngestionRequest;
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
    public Mono<ResponseEntity<TransactionIngestionResponse>> pullIngestTransactions(
            @Valid Mono<TransactionPullIngestionRequest> pullIngestionRequest, ServerWebExchange exchange) {
        return transactionIngestionService
                .ingestPull(pullIngestionRequest.map(this::buildPullRequest))
                .map(this::mapIngestionToResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<TransactionIngestionResponse>> pushIngestTransactions(
            @Valid Mono<TransactionPushIngestionRequest> pushIngestionRequest, ServerWebExchange exchange) {
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
    private TransactionIngestPullRequest buildPullRequest(TransactionPullIngestionRequest request) {
        return TransactionIngestPullRequest
                .builder()
                .userExternalId(request.getUserExternalId())
                .productGroup(request.getProductGroup())
                .additionalParameters(request.getAdditionalParameters())
                .build();
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PushIngestionRequest
     * @return ProductIngestPushRequest
     */
    private TransactionIngestPushRequest buildPushRequest(TransactionPushIngestionRequest request) {
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
    private ResponseEntity<TransactionIngestionResponse> mapIngestionToResponse(TransactionIngestResponse response) {
        return new ResponseEntity<>(
                new TransactionIngestionResponse()
                        .withTransactions(
                                response.getTransactions().stream().map(item -> mapper.mapStreamToComposition(item)).collect(Collectors.toList())),
                HttpStatus.CREATED);
    }
}
