package com.backbase.stream.compositions.transaction.core.mapper;

import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

/**
 * This is a mapper for TransactionsPostRequestBody objects used in:
 * - dbs model
 * - transaction-composition-api
 * - transaction-integration-api
 * - transaction-events
 * <p>
 * All TransactionsPostRequestBody objects used in above modules have exactly same structures they are built
 * from the common /api folder.
 */
@Mapper
@Component
public interface TransactionMapper {
    /**
     * Maps composition TransactionsPostRequestBody to dbs TransactionsPostRequestBody model.
     *
     * @param transaction Integration transaction
     * @return DBS transaction
     */
    com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostRequestBody mapIntegrationToStream(
            com.backbase.stream.compositions.integration.transaction.model.TransactionsPostRequestBody transaction);

    /**
     * Maps integration TransactionsPostRequestBody to composition TransactionsPostRequestBody model.
     *
     * @param transaction Integration transaction
     * @return Composition transaction
     */
    com.backbase.stream.compositions.transaction.model.TransactionsPostRequestBody mapIntegrationToComposition(
            com.backbase.stream.compositions.integration.transaction.model.TransactionsPostRequestBody transaction);

    /**
     * Maps composition TransactionsPostRequestBody to dbs TransactionsPostRequestBody model.
     *
     * @param transaction Composition transaction
     * @return DBS transaction
     */
    com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostRequestBody mapCompositionToStream(
            com.backbase.stream.compositions.transaction.model.TransactionsPostRequestBody transaction);

    /**
     * Maps dbs TransactionsPostRequestBody to composition TransactionsPostRequestBody model.
     *
     * @param transaction DBS transaction
     * @return Composition transaction
     */
    com.backbase.stream.compositions.transaction.model.TransactionsPostResponseBody mapStreamToComposition(
            com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostResponseBody transaction);

    /**
     * Maps event Transactions to dbs TransactionsPostRequestBody model.
     *
     * @param transaction Event transaction
     * @return Stream transaction
     */
    com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostRequestBody mapEventToStream(
            com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.Transaction transaction);
}
