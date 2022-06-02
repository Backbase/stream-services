package com.backbase.stream.compositions.transaction.http;

import com.backbase.stream.compositions.transaction.api.TransactionCompositionApi;
import com.backbase.stream.compositions.transaction.api.model.TransactionIngestionResponse;
import com.backbase.stream.compositions.transaction.api.model.TransactionPullIngestionRequest;
import com.backbase.stream.compositions.transaction.api.model.TransactionPushIngestionRequest;
import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPullRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPushRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestResponse;
import com.backbase.stream.compositions.transaction.core.service.TransactionIngestionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.validation.Valid;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@Slf4j
public class TransactionController implements TransactionCompositionApi {
    private final TransactionIngestionService transactionIngestionService;
    private final TransactionMapper mapper;

    @Override
    public Mono<ResponseEntity<TransactionIngestionResponse>> pullTransactions(Mono<TransactionPullIngestionRequest> pullIngestionRequest, ServerWebExchange exchange) {
        return transactionIngestionService
                .ingestPull(pullIngestionRequest.map(this::buildPullRequest))
                .map(this::mapIngestionToResponse);
    }

    @Override
    public Mono<ResponseEntity<Void>> pullTransactionsAsync(Mono<TransactionPullIngestionRequest> pullIngestionRequest, ServerWebExchange exchange) {
        Mono.fromCallable(() -> transactionIngestionService
                .ingestPull(pullIngestionRequest.map(this::buildPullRequest))
                .map(this::mapIngestionToResponse).subscribeOn(Schedulers.boundedElastic()).subscribe());
        return Mono.empty();
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
                .externalArrangementId(request.getExternalArrangementId())
                .dateRangeStart(request.getDateRangeStart())
                .dateRangeEnd(request.getDateRangeEnd())
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
                        request.getTransactions().stream().map(mapper::mapCompositionToStream)
                                .collect(Collectors.toList()))
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
                                response.getTransactions().stream().map(mapper::mapStreamToComposition).collect(Collectors.toList())),
                HttpStatus.CREATED);
    }
}
