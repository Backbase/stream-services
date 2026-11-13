package com.backbase.stream.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import com.backbase.dbs.transaction.api.service.v4.TransactionPresentationServiceApi;
import com.backbase.dbs.transaction.api.service.v4.model.TransactionsPostRequestBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TransactionTaskExecutorTest {

    private TransactionTaskExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new TransactionTaskExecutor(mock(TransactionPresentationServiceApi.class));
    }

    @Test
    void ensureSequenceNumber_nullBody_noOp() {
        assertDoesNotThrow(() -> executor.ensureSequenceNumber(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "42", "99999999999999999999"})
    void ensureSequenceNumber_keepsValidValue(String sequenceNumber) {
        TransactionsPostRequestBody body = new TransactionsPostRequestBody()
            .sequenceNumber(sequenceNumber)
            .externalId("ext-1");

        executor.ensureSequenceNumber(body);

        assertEquals(sequenceNumber, body.getSequenceNumber());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "-1", "01", "10a", "abc"})
    void ensureSequenceNumber_fallsBackToNumericExternalId(String sequenceNumber) {
        TransactionsPostRequestBody body = new TransactionsPostRequestBody()
            .sequenceNumber(sequenceNumber)
            .externalId("42");

        executor.ensureSequenceNumber(body);

        assertEquals("42", body.getSequenceNumber());
    }

    @Test
    void ensureSequenceNumber_defaultsToZeroWhenNoValidCandidate() {
        TransactionsPostRequestBody body = new TransactionsPostRequestBody()
            .sequenceNumber(null)
            .externalId("not-numeric");

        executor.ensureSequenceNumber(body);

        assertEquals("0", body.getSequenceNumber());
    }

    @Test
    void ensureSequenceNumber_rejectsExternalIdLongerThan20() {
        String tooLong = "1".repeat(21);
        TransactionsPostRequestBody body = new TransactionsPostRequestBody()
            .sequenceNumber(null)
            .externalId(tooLong);

        executor.ensureSequenceNumber(body);

        assertEquals("0", body.getSequenceNumber());
    }

    @Test
    void ensureSequenceNumber_prefersExistingOverExternalId() {
        TransactionsPostRequestBody body = new TransactionsPostRequestBody()
            .sequenceNumber("123")
            .externalId("999");

        executor.ensureSequenceNumber(body);

        assertEquals("123", body.getSequenceNumber());
    }
}
