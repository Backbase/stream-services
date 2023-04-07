package com.backbase.stream.transactions;

import com.backbase.stream.configuration.TransactionServiceConfiguration;
import com.backbase.stream.transaction.TransactionUnitOfWorkExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(TransactionServiceConfiguration.class)
public class TransactionItemWriterConfiguration {

  @Bean
  public TransactionsItemWriter transactionsItemWriter(
      TransactionUnitOfWorkExecutor transactionUnitOfWorkExecutor) {
    return new TransactionsItemWriter(transactionUnitOfWorkExecutor);
  }
}
