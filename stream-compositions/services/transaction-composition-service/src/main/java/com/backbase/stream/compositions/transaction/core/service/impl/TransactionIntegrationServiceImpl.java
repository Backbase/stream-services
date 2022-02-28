package com.backbase.stream.compositions.transaction.core.service.impl;

import com.backbase.stream.compositions.integration.transaction.api.TransactionIntegrationApi;
import com.backbase.stream.compositions.integration.transaction.model.PullIngestionRequest;
import com.backbase.stream.compositions.integration.transaction.model.PullTransactionsResponse;
import com.backbase.stream.compositions.integration.transaction.model.TransactionsPostRequestBody;
import com.backbase.stream.compositions.transaction.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPullRequest;
import com.backbase.stream.compositions.transaction.core.service.TransactionIntegrationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@AllArgsConstructor
public class TransactionIntegrationServiceImpl implements TransactionIntegrationService {
    private final TransactionIntegrationApi transactionIntegrationApi;
    private final ProductGroupMapper productGroupMapper;

    /**
     * {@inheritDoc}
     */
    public Flux<TransactionsPostRequestBody> pullTransactions(TransactionIngestPullRequest ingestPullRequest) {
        return transactionIntegrationApi
                .pullTransactions(
                        new PullIngestionRequest()
                                .userExternalId(ingestPullRequest.getUserExternalId())
                                .productGroup(productGroupMapper.mapCompositionToIntegration(ingestPullRequest.getProductGroup())))
                .flatMapIterable(PullTransactionsResponse::getTransactions);
    }
}
