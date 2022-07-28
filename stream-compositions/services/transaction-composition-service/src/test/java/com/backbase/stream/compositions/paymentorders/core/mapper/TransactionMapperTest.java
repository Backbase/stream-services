package com.backbase.stream.compositions.paymentorders.core.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionMapperTest {

  TransactionMapper transactionMapper = Mappers.getMapper(TransactionMapper.class);

  @Test
  void testMapperWithNull(){
    OffsetDateTime offsetDateTime = transactionMapper.map(null);
    assertThat(offsetDateTime).isNull();
  }

  @Test
  void testMapperWithDateTimeFormatter(){
    OffsetDateTime offsetDateTime = transactionMapper.map(OffsetDateTime.now().format(
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX")));
    assertThat(offsetDateTime).isNotNull();
  }

 // @Test
  void testMapperWithDateTimeException() throws DateTimeParseException{
    String localDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'"));
    String offsetDateTime = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
    transactionMapper.map(offsetDateTime);
    assertThat(offsetDateTime).isNotNull();
  }
}
