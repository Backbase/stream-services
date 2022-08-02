package com.backbase.stream.compositions.paymentorders.core.mapper;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostResponse;
import com.backbase.stream.compositions.paymentorder.integration.client.model.PullIngestionRequest;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestPullRequest;
import org.mapstruct.InjectionStrategy;
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
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
@Component
public interface PaymentOrderMapper {

//    String YYYY_MM_DD_T_HH_MM_SS_SSSXX = "yyyy-MM-dd'T'HH:mm:ss.SSSXX";
//    DateTimeFormatter formatter =
//            DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_SSSXX);
//
    /**
     * Maps composition PaymentOrderPostRequestBody to dbs PaymentOrderPostRequestBody model.
     *
     * @param PaymentOrderPostRequest
     * @return DBS Payment Order
     */
    PaymentOrderPostRequest mapIntegrationToStream(
            com.backbase.stream.compositions.paymentorder.integration.client.model.PaymentOrderPostRequest paymentOrderPostRequest);
//
//    /**
//     * Maps integration TransactionsPostRequestBody to composition TransactionsPostRequestBody model.
//     *
//     * @param transaction Integration transaction
//     * @return Composition transaction
//     */
//    com.backbase.stream.compositions.paymentorders.api.model.TransactionsPostRequestBody mapIntegrationToComposition(
//            com.backbase.stream.compositions.paymentorders.integration.client.model.TransactionsPostRequestBody transaction);
//
//    /**
//     * Maps composition TransactionsPostRequestBody to dbs TransactionsPostRequestBody model.
//     *
//     * @param transaction Composition transaction
//     * @return DBS transaction
//     */
//    com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostRequestBody mapCompositionToStream(
//            com.backbase.stream.compositions.paymentorders.api.model.TransactionsPostRequestBody transaction);
//
//    /**
//     * Maps dbs TransactionsPostRequestBody to composition TransactionsPostRequestBody model.
//     *
//     * @param transaction DBS transaction
//     * @return Composition transaction
//     */
//    com.backbase.stream.compositions.paymentorders.api.model.PaymentOrderPostResponse mapStreamToComposition(
//            com.backbase.dbs.transaction.api.service.v2.model.PaymentOrderPostResponseBody paymentOrderPostResponseBody);

     com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPostResponse mapStreamToComposition(PaymentOrderPostResponse paymentOrderPostResponse);

//    /**
//     * Maps event Transactions to dbs TransactionsPostRequestBody model.
//     *
//     * @param transaction Event transaction
//     * @return Stream transaction
//     */
//    com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostRequestBody mapPushEventToStream(
//            TransactionsPostRequestBody transaction);
//
//    /**
//     * Maps pull event Transactions to Stream TransactionIngestPullRequest model.
//     *
//     * @param event Pull Event transaction
//     * @return Stream transaction pull object
//     */
//    TransactionIngestPullRequest mapPullEventToStream(
//            TransactionsPullEvent event);
//
    /**
     * Maps Stream Pull Ingestion Request to Integration.
     *
     * @param model Stream Pull Ingestion Request
     * @return Integration Pull Ingestion Request
     */
    PullIngestionRequest mapStreamToIntegration(PaymentOrderIngestPullRequest model);
//
//    default OffsetDateTime map(String s) {
//        if (!StringUtils.hasLength(s)) {
//            return null;
//        } else {
//            try {
//                return OffsetDateTime.parse(s, formatter);
//            } catch (java.time.format.DateTimeParseException e) {
//                return OffsetDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'"));
//            }
//        }
//    }

}
