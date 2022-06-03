package com.backbase.stream.compositions.transaction.core.service.impl;

import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostRequestBody;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostResponseBody;
import com.backbase.stream.TransactionService;
import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPullRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPushRequest;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestResponse;
import com.backbase.stream.compositions.transaction.core.service.TransactionIngestionService;
import com.backbase.stream.compositions.transaction.core.service.TransactionIntegrationService;
import com.backbase.stream.compositions.transaction.core.service.TransactionPostIngestionService;
import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.worker.model.UnitOfWork;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class TransactionIngestionServiceImpl implements TransactionIngestionService {
    private final TransactionMapper mapper;
    private final TransactionService transactionService;
    private final TransactionIntegrationService transactionIntegrationService;

    private final TransactionPostIngestionService transactionPostIngestionService;

    /**
     * Ingests transactions in pull mode.
     *
     * @param ingestPullRequest Ingest pull request
     * @return TransactionIngestResponse
     */
    public Mono<TransactionIngestResponse> ingestPull(Mono<TransactionIngestPullRequest> ingestPullRequest) {
        return ingestPullRequest
                .map(this::pullTransactions)
                .flatMap(this::sendToDbs)
                .doOnSuccess(transactionPostIngestionService::handleSuccess)
                .onErrorResume(transactionPostIngestionService::handleFailure)
                .map(this::buildResponse);
    }

    /**
     * Ingests product group in push mode.
     *
     * @param ingestPushRequest Ingest push request
     * @return ProductIngestResponse
     */
    public Mono<TransactionIngestResponse> ingestPush(Mono<TransactionIngestPushRequest> ingestPushRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Pulls and remap product group from integration service.
     *
     * @param request TransactionIngestPullRequest
     * @return Flux<TransactionsPostRequestBody>
     */
    private Flux<TransactionsPostRequestBody> pullTransactions(TransactionIngestPullRequest request) {
        /**
         * TODO: Call to GET /cursor/arrangement/{id}
         * If the cursor does not exist, call upsert IN_PROGRESS
         * If the cursor exists, call PATCH IN_PROGRESS
         * Get call gives you  startDate (last_txn_date). The endDate is always (now == current_timestamp)
         * Set Default startDate in application.yml (30 days), i.e. now() - 30 days
         *
         */


        return transactionIntegrationService.pullTransactions(request)
                .map(mapper::mapIntegrationToStream);
    }

    private Mono<TransactionIngestPullRequest> getCursor(TransactionIngestPullRequest request) {
        return Mono.empty();
    }

    /**
     * Ingests transactions to DBS.
     *
     * @param transactions Transactions
     * @return Ingested transactions
     */
    private Mono<List<TransactionsPostResponseBody>> sendToDbs(Flux<TransactionsPostRequestBody> transactions) {
        return transactions
                .publish(transactionService::processTransactions)
                .flatMapIterable(UnitOfWork::getStreamTasks)
                .flatMapIterable(TransactionTask::getResponse)
                .collectList();
    }

    private TransactionIngestResponse buildResponse(List<TransactionsPostResponseBody> transactions) {
        return TransactionIngestResponse.builder()
                .transactions(transactions)
                .build();
    }

    private void handleSuccess(List<TransactionsPostResponseBody> transactions) {
        log.error("Transactions ingestion completed");
        if (log.isDebugEnabled()) {
            log.debug("Ingested transactions: {}", transactions);
        }
    }
}
