package com.backbase.stream.compositions.product.core.service.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties.Chains;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties.Cursor;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties.Events;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties.PaymentOrderComposition;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties.TransactionComposition;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductConfigurationPropertiesTest {

  @Test
  void testConfigurationProperties() {
    ProductConfigurationProperties properties = new ProductConfigurationProperties();
    properties.setIntegrationBaseUrl("https://product");

    Chains chains = new Chains();
    TransactionComposition transactionComposition = new TransactionComposition();
    transactionComposition.setEnabled(Boolean.TRUE);
    transactionComposition.setBaseUrl("https://transaction-composition");
    transactionComposition.setAsync(Boolean.TRUE);
    transactionComposition.setExcludeProductTypeExternalIds(List.of());
    chains.setTransactionComposition(transactionComposition);

    PaymentOrderComposition paymentOrderComposition = new PaymentOrderComposition();
    paymentOrderComposition.setEnabled(Boolean.TRUE);
    paymentOrderComposition.setBaseUrl("https://payment-order-composition");
    paymentOrderComposition.setAsync(Boolean.TRUE);
    paymentOrderComposition.setExcludeProductTypeExternalIds(List.of());
    chains.setPaymentOrderComposition(paymentOrderComposition);

    Events events = new Events();
    events.setEnableCompleted(Boolean.TRUE);
    events.setEnableFailed(Boolean.TRUE);

    Cursor cursor = new Cursor();
    cursor.setBaseUrl("https://cursor");
    cursor.setEnabled(Boolean.TRUE);

    properties.setChains(chains);
    properties.setEvents(events);
    properties.setCursor(cursor);

    assertTrue(properties.getIntegrationBaseUrl().contains("product"), "Correct config spotted");
    assertTrue(
        properties
            .getChains()
            .getTransactionComposition()
            .getBaseUrl()
            .contains("transaction-composition"),
        "Correct config spotted");
    assertTrue(
        properties
            .getChains()
            .getPaymentOrderComposition()
            .getBaseUrl()
            .contains("payment-order-composition"),
        "Correct config spotted");
    assertTrue(properties.getEvents().getEnableCompleted());
    assertTrue(properties.getEvents().getEnableFailed());
    assertTrue(properties.getCursor().getEnabled());
    assertTrue(properties.getCursor().getBaseUrl().contains("cursor"), "Correct config spotted");
    assertTrue(properties.isCompletedEventEnabled());
    assertTrue(properties.isFailedEventEnabled());
    assertTrue(properties.isTransactionChainEnabled());
    assertTrue(properties.isTransactionChainAsync());
    assertTrue(properties.isPaymentOrderChainEnabled());
    assertTrue(properties.isPaymentOrderChainAsync());
  }
}
