package com.backbase.stream.compositions.transaction.core.service.impl;

import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.stream.TransactionService;
import com.backbase.stream.compositions.transaction.core.config.TransactionConfigurationProperties;
import com.backbase.stream.compositions.transaction.core.mapper.TransactionMapper;
import com.backbase.stream.compositions.transaction.core.service.TransactionIngestionService;
import com.backbase.stream.compositions.transaction.core.service.TransactionIntegrationService;
import com.backbase.stream.compositions.transaction.core.service.TransactionPostIngestionService;
import com.backbase.stream.compositions.transaction.cursor.client.TransactionCursorApi;
import javax.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionIngestionServiceImplTest {

  private TransactionIngestionService transactionIngestionService;

  @Mock
  private TransactionIntegrationService transactionIntegrationService;

  @Mock
  Validator validator;

  TransactionPostIngestionService transactionPostIngestionService;

  TransactionConfigurationProperties config = new TransactionConfigurationProperties();

  @Mock
  EventBus eventBus;

  @Mock
  TransactionService transactionService;
  @Mock
  TransactionCursorApi transactionCursorApi;

  TransactionMapper mapper = Mappers.getMapper(TransactionMapper.class);

  @BeforeEach
  void setUp() {
    transactionPostIngestionService = new TransactionPostIngestionServiceImpl(eventBus, config);

    transactionIngestionService = new TransactionIngestionServiceImpl(mapper,
        transactionService, transactionIntegrationService, transactionPostIngestionService,
        transactionCursorApi, config);
  }

}
