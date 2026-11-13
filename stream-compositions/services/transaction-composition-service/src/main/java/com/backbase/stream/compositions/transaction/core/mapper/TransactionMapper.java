package com.backbase.stream.compositions.transaction.core.mapper;

import com.backbase.stream.compositions.events.ingress.event.spec.v1.TransactionsPostRequestBody;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.TransactionsPullEvent;
import com.backbase.stream.compositions.transaction.core.model.TransactionIngestPullRequest;
import com.backbase.stream.compositions.transaction.integration.client.model.PullIngestionRequest;
import org.mapstruct.AfterMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

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
public interface TransactionMapper {

    String YYYY_MM_DD_T_HH_MM_SS_SSSXX = "yyyy-MM-dd'T'HH:mm:ss.SSSXX";
    DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_SSSXX);
    /** Aligns with service-api v4: `(^[1-9][0-9]*)|0$` as a full-string match, maxLength 20. */
    Pattern SEQUENCE_NUMBER_PATTERN = Pattern.compile("^(?:0|[1-9]\\d{0,19})$");
    int SEQUENCE_NUMBER_MAX_LENGTH = 20;

    /**
     * Maps composition TransactionsPostRequestBody to dbs TransactionsPostRequestBody model.
     *
     * @param transaction Integration transaction
     * @return DBS transaction
     */
    com.backbase.dbs.transaction.api.service.v4.model.TransactionsPostRequestBody mapIntegrationToStream(
            com.backbase.stream.compositions.transaction.integration.client.model.TransactionsPostRequestBody transaction);

    /**
     * Maps integration TransactionsPostRequestBody to composition TransactionsPostRequestBody model.
     *
     * @param transaction Integration transaction
     * @return Composition transaction
     */
    com.backbase.stream.compositions.transaction.api.model.TransactionsPostRequestBody mapIntegrationToComposition(
            com.backbase.stream.compositions.transaction.integration.client.model.TransactionsPostRequestBody transaction);

    /**
     * Maps composition TransactionsPostRequestBody to dbs TransactionsPostRequestBody model.
     *
     * @param transaction Composition transaction
     * @return DBS transaction
     */
    com.backbase.dbs.transaction.api.service.v4.model.TransactionsPostRequestBody mapCompositionToStream(
            com.backbase.stream.compositions.transaction.api.model.TransactionsPostRequestBody transaction);

    /**
     * Maps dbs TransactionsPostRequestBody to composition TransactionsPostRequestBody model.
     *
     * @param transaction DBS transaction
     * @return Composition transaction
     */
    com.backbase.stream.compositions.transaction.api.model.TransactionsPostResponseBody mapStreamToComposition(
            com.backbase.dbs.transaction.api.service.v4.model.TransactionsPostResponseBody transaction);

    /**
     * Maps event Transactions to dbs TransactionsPostRequestBody model.
     *
     * @param transaction Event transaction
     * @return Stream transaction
     */
    com.backbase.dbs.transaction.api.service.v4.model.TransactionsPostRequestBody mapPushEventToStream(
            TransactionsPostRequestBody transaction);

    /**
     * service-api v4 requires sequenceNumber. Prefer existing value; else numeric externalId; else "0".
     */
    @AfterMapping
    default void ensureSequenceNumber(
            @MappingTarget com.backbase.dbs.transaction.api.service.v4.model.TransactionsPostRequestBody target) {
        if (target == null) {
            return;
        }
        if (isValidSequenceNumber(target.getSequenceNumber())) {
            return;
        }
        String externalId = target.getExternalId();
        if (isValidSequenceNumber(externalId)) {
            target.setSequenceNumber(externalId);
            return;
        }
        target.setSequenceNumber("0");
    }

    default boolean isValidSequenceNumber(String value) {
        return StringUtils.hasLength(value)
            && value.length() <= SEQUENCE_NUMBER_MAX_LENGTH
            && SEQUENCE_NUMBER_PATTERN.matcher(value).matches();
    }

    /**
     * Maps pull event Transactions to Stream TransactionIngestPullRequest model.
     *
     * @param event Pull Event transaction
     * @return Stream transaction pull object
     */
    TransactionIngestPullRequest mapPullEventToStream(
            TransactionsPullEvent event);

    /**
     * Maps Stream Pull Ingestion Request to Integration.
     *
     * @param model Stream Pull Ingestion Request
     * @return Integration Pull Ingestion Request
     */
    PullIngestionRequest mapStreamToIntegration(TransactionIngestPullRequest model);

    default OffsetDateTime map(String s) {
        if (!StringUtils.hasLength(s)) {
            return null;
        } else {
            try {
                return OffsetDateTime.parse(s, formatter);
            } catch (java.time.format.DateTimeParseException e) {
                return OffsetDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'"));
            }
        }
    }

}
