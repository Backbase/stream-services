package com.backbase.stream.compositions.transaction.core.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.backbase.dbs.transaction.api.service.v4.model.TransactionsPostRequestBody;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionMapperTest {

    TransactionMapper transactionMapper = Mappers.getMapper(TransactionMapper.class);

    @Test
    void testMapperWithNull() {
        OffsetDateTime offsetDateTime = transactionMapper.map(null);
        assertThat(offsetDateTime).isNull();
    }

    @Test
    void testMapperWithDateTimeFormatter() {
        OffsetDateTime offsetDateTime = transactionMapper.map(OffsetDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX")));
        assertThat(offsetDateTime).isNotNull();
    }

    // @Test
    void testMapperWithDateTimeException() throws DateTimeParseException {
        String localDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'"));
        String offsetDateTime = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        transactionMapper.map(offsetDateTime);
        assertThat(offsetDateTime).isNotNull();
    }

    @Test
    void ensureSequenceNumber_nullBody_noOp() {
        assertDoesNotThrow(() -> transactionMapper.ensureSequenceNumber(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "42", "99999999999999999999"})
    void ensureSequenceNumber_keepsValidValue(String sequenceNumber) {
        TransactionsPostRequestBody body = new TransactionsPostRequestBody()
            .sequenceNumber(sequenceNumber)
            .externalId("ext-1");

        transactionMapper.ensureSequenceNumber(body);

        assertEquals(sequenceNumber, body.getSequenceNumber());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "-1", "01", "10a", "abc"})
    void ensureSequenceNumber_fallsBackToNumericExternalId(String sequenceNumber) {
        TransactionsPostRequestBody body = new TransactionsPostRequestBody()
            .sequenceNumber(sequenceNumber)
            .externalId("42");

        transactionMapper.ensureSequenceNumber(body);

        assertEquals("42", body.getSequenceNumber());
    }

    @Test
    void ensureSequenceNumber_defaultsToZeroWhenNoValidCandidate() {
        TransactionsPostRequestBody body = new TransactionsPostRequestBody()
            .sequenceNumber(null)
            .externalId("not-numeric");

        transactionMapper.ensureSequenceNumber(body);

        assertEquals("0", body.getSequenceNumber());
    }

    @Test
    void ensureSequenceNumber_rejectsExternalIdLongerThan20() {
        String tooLong = "1".repeat(21);
        TransactionsPostRequestBody body = new TransactionsPostRequestBody()
            .sequenceNumber(null)
            .externalId(tooLong);

        transactionMapper.ensureSequenceNumber(body);

        assertEquals("0", body.getSequenceNumber());
    }

    @Test
    void ensureSequenceNumber_prefersExistingOverExternalId() {
        TransactionsPostRequestBody body = new TransactionsPostRequestBody()
            .sequenceNumber("123")
            .externalId("999");

        transactionMapper.ensureSequenceNumber(body);

        assertEquals("123", body.getSequenceNumber());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "42", "99999999999999999999"})
    void isValidSequenceNumber_acceptsValidValues(String value) {
        assertTrue(transactionMapper.isValidSequenceNumber(value));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "-1", "01", "10a", "abc"})
    void isValidSequenceNumber_rejectsInvalidValues(String value) {
        assertFalse(transactionMapper.isValidSequenceNumber(value));
    }

    @Test
    void isValidSequenceNumber_rejectsValueLongerThan20() {
        assertFalse(transactionMapper.isValidSequenceNumber("1".repeat(21)));
    }
}
