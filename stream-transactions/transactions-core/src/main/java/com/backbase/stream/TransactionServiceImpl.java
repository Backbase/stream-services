package com.backbase.stream;

import com.backbase.dbs.transaction.api.service.v3.TransactionPresentationServiceApi;
import com.backbase.dbs.transaction.api.service.v3.model.ArrangementItem;
import com.backbase.dbs.transaction.api.service.v3.model.TransactionItem;
import com.backbase.dbs.transaction.api.service.v3.model.TransactionsDeleteRequestBody;
import com.backbase.dbs.transaction.api.service.v3.model.TransactionsPatchRequestBody;
import com.backbase.dbs.transaction.api.service.v3.model.TransactionsPostRequestBody;
import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.transaction.TransactionUnitOfWorkExecutor;
import com.backbase.stream.transaction.TransactionsQuery;
import com.backbase.stream.worker.model.UnitOfWork;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Main Transaction Ingestion Service. Supports Retry and back pressure and controller number of transactions to ingest
 * per second.
 */
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionPresentationServiceApi transactionPresentationServiceApi;
    private final TransactionUnitOfWorkExecutor transactionTaskExecutor;

    public TransactionServiceImpl(TransactionPresentationServiceApi transactionPresentationServiceApi,
                                  TransactionUnitOfWorkExecutor transactionTaskExecutor) {
        this.transactionTaskExecutor = transactionTaskExecutor;
        this.transactionPresentationServiceApi = transactionPresentationServiceApi;
    }

    /**
     * Upsert Transactions.
     *
     * @param transactions Unbounded list of Transactions
     * @return Ingestion Transactions IDs
     */
    @Override
    public Flux<UnitOfWork<TransactionTask>> processTransactions(Flux<TransactionsPostRequestBody> transactions) {
        Flux<UnitOfWork<TransactionTask>> unitOfWorkFlux = transactionTaskExecutor.prepareUnitOfWork(transactions);
        return unitOfWorkFlux.flatMap(transactionTaskExecutor::executeUnitOfWork);
    }


    /**
     * Retrieve latest transactions for an Arrangement.
     *
     * @param arrangementId external productId
     * @param size number of transactions to return
     * @param bookingDateGreaterThan booking date greater than (ISO-8601 format with timezone)
     * @param bookingDateLessThan booking date less than (ISO-8601 format with timezone)
     * @return List of transactions
     */
    @Override
    public Flux<TransactionItem> getLatestTransactions(String arrangementId, int size, String bookingDateGreaterThan, String bookingDateLessThan) {
        TransactionsQuery transactionsQuery = new TransactionsQuery();
        transactionsQuery.setArrangementId(arrangementId);
        transactionsQuery.setSize(size);
        transactionsQuery.setBookingDateGreaterThan(bookingDateGreaterThan);
        transactionsQuery.setBookingDateLessThan(bookingDateLessThan);
        return getTransactions(transactionsQuery)
            .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                log.info("No transactions found for: {} message: {}", arrangementId, ex.getResponseBodyAsString());
                return Flux.empty();
            }).doOnError(Throwable.class, ex -> log.error("Error: {}", ex.getMessage(), ex));
    }


    /**
     * Remove transactions from DBS.
     *
     * @param transactionItemDelete Body of transactions to delete
     * @return Void if successful.
     */
    @Override
    @SuppressWarnings("WeakerAccess")
    public Mono<Void> deleteTransactions(Flux<TransactionsDeleteRequestBody> transactionItemDelete) {
        return transactionItemDelete
            .collectList()
            .flatMap(item -> transactionPresentationServiceApi.postDelete(item, null, null, null));

    }

    /**
     * Get list of transactions.
     *
     * @param transactionsQuery Transaction Query
     * @return A list of transactions
     */
    @Override
    public Flux<TransactionItem> getTransactions(TransactionsQuery transactionsQuery) {
        return transactionPresentationServiceApi.getTransactions(
            transactionsQuery.getBookingDateGreaterThan(), // 1. String bookingDateGreaterThan
            transactionsQuery.getBookingDateLessThan(),    // 2. String bookingDateLessThan
            null,                                         // 3. String xTransactionsUserId
            null,                                         // 4. String xTransactionsInternalUserId (NEW in v3)
            null,                                         // 5. String xTransactionsServiceAgreementId
            transactionsQuery.getAmountGreaterThan(),    // 6. BigDecimal amountGreaterThan
            transactionsQuery.getAmountLessThan(),        // 7. BigDecimal amountLessThan
            transactionsQuery.getTypes(),                  // 8. List<String> types
            transactionsQuery.getDescription(),           // 9. String description
            transactionsQuery.getReference(),              // 10. String reference
            transactionsQuery.getTypeGroups(),             // 11. List<String> typeGroups
            transactionsQuery.getCounterPartyName(),      // 12. String counterPartyName
            transactionsQuery.getCounterPartyAccountNumber(), // 13. String counterPartyAccountNumber
            transactionsQuery.getCreditDebitIndicator(),  // 14. String creditDebitIndicator
            transactionsQuery.getCategories(),            // 15. List<String> categories
            transactionsQuery.getBillingStatus(),         // 16. String billingStatus
            transactionsQuery.getState(),                 // 17. TransactionState state
            transactionsQuery.getCurrency(),              // 18. String currency
            transactionsQuery.getNotes(),                 // 19. Integer notes
            transactionsQuery.getId(),                    // 20. String id
            transactionsQuery.getArrangementId(),         // 21. String arrangementId
            transactionsQuery.getArrangementsIds(),      // 22. List<String> arrangementsIds
            transactionsQuery.getFromCheckSerialNumber(), // 23. Long fromCheckSerialNumber
            transactionsQuery.getToCheckSerialNumber(),  // 24. Long toCheckSerialNumber
            transactionsQuery.getCheckSerialNumbers(),   // 25. List<Long> checkSerialNumbers
            transactionsQuery.getQuery(),                 // 26. String query
            transactionsQuery.getFrom(),                 // 27. Integer from
            transactionsQuery.getCursor(),                // 28. String cursor
            transactionsQuery.getSize(),                  // 29. Integer size
            transactionsQuery.getOrderBy(),               // 30. String orderBy
            transactionsQuery.getDirection(),            // 31. String direction
            transactionsQuery.getSecDirection());          // 32. String secDirection
    }

    /**
     * Update Transactions  with a new category or billing status.
     *
     * @param transactionItems Updated category and billing status fields
     * @return empty mono on completion
     */
    @Override
    public Mono<Void> patchTransactions(Flux<TransactionsPatchRequestBody> transactionItems) {
        return transactionItems
            .collectList()
            .flatMap(items -> transactionPresentationServiceApi.patchTransactions(items, null, null, null));
    }

    /**
     * Trigger refresh action for transactions.
     *
     * @param arrangementItems Arrangement ids for which to retrieve new transactions
     * @return empty mono on completion
     */
    @Override
    public Mono<Void> postRefresh(Flux<ArrangementItem> arrangementItems) {
        return arrangementItems
            .collectList()
            .flatMap(item -> transactionPresentationServiceApi.postRefresh(item, null, null, null));
    }

}
