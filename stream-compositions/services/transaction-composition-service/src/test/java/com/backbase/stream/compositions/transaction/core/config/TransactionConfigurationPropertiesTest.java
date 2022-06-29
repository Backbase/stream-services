package com.backbase.stream.compositions.transaction.core.config;

import com.backbase.stream.compositions.transaction.core.config.TransactionConfigurationProperties.Cursor;
import com.backbase.stream.compositions.transaction.core.config.TransactionConfigurationProperties.Events;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TransactionConfigurationPropertiesTest {

  @Test
  void testConfig() {
    TransactionConfigurationProperties properties = new TransactionConfigurationProperties();
    properties.setIntegrationBaseUrl("https://transaction");
    properties.setDefaultStartOffsetInDays(30);

    Events events = new Events();
    events.setEnableCompleted(Boolean.TRUE);
    events.setEnableFailed(Boolean.TRUE);

    Cursor cursor = new Cursor();
    cursor.setBaseUrl("https://transaction-cursor");
    cursor.setTransactionIdsFilterEnabled(Boolean.FALSE);
    cursor.setEnabled(Boolean.TRUE);

    properties.setEvents(events);
    properties.setCursor(cursor);

    assertTrue(properties.getIntegrationBaseUrl().contains("transaction"),
        "Correct config spotted");
    assertEquals(30, properties.getDefaultStartOffsetInDays());
    assertTrue(properties.getEvents().getEnableCompleted());
    assertTrue(properties.getEvents().getEnableFailed());
    assertTrue(properties.getCursor().getEnabled());
    assertFalse(properties.getCursor().getTransactionIdsFilterEnabled());
    assertTrue(properties.getCursor().getBaseUrl().contains("transaction-cursor"), "Correct config spotted");
    assertTrue(properties.isCursorEnabled());
    assertFalse(properties.isTransactionIdsFilterEnabled());
  }
}
