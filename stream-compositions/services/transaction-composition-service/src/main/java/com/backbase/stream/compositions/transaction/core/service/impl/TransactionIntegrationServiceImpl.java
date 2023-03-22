package com.backbase.stream.compositions.transaction.core.service.impl;

import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPullRequest;
import com.backbase.stream.compositions.transaction.core.service.TransactionIntegrationService;
import com.backbase.stream.compositions.transaction.integration.client.TransactionIntegrationApi;
import com.backbase.stream.compositions.transaction.integration.client.model.PullTransactionsResponse;
import com.backbase.stream.compositions.transaction.integration.client.model.TransactionsPostRequestBody;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@AllArgsConstructor
public class TransactionIntegrationServiceImpl implements TransactionIntegrationService {
    private final TransactionIntegrationApi transactionIntegrationApi;
    private final TransactionMapper transactionMapper;

    /**
     * {@inheritDoc}
     */
    public Flux<TransactionsPostRequestBody> pullTransactions(TransactionIngestPullRequest ingestPullRequest) {
        return transactionIntegrationApi
                .pullTransactions(
                        transactionMapper.mapStreamToIntegration(ingestPullRequest))
                .doOnNext(response -> processSuccess(response, ingestPullRequest))
                .flatMapIterable(PullTransactionsResponse::getTransactions);
    }

    private void processSuccess(PullTransactionsResponse pullTransactionsResponse, TransactionIngestPullRequest ingestPullRequest) {
        propagateAdditions(pullTransactionsResponse, ingestPullRequest);
    }

    private void propagateAdditions(PullTransactionsResponse pullTransactionsResponse,
            TransactionIngestPullRequest ingestPullRequest) {
        if (pullTransactionsResponse.getAdditions() != null) {
            Map<String, String> additions = ingestPullRequest.getAdditions();
            if (additions == null) {
                additions = new HashMap<>();
            }
            additions.putAll(pullTransactionsResponse.getAdditions());
            ingestPullRequest.setAdditions(additions);
        }
    }
}
