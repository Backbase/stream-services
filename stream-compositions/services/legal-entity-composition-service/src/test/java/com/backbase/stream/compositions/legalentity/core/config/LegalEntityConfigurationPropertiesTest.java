package com.backbase.stream.compositions.legalentity.core.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.backbase.stream.compositions.legalentity.core.config.LegalEntityConfigurationProperties.Chains;
import com.backbase.stream.compositions.legalentity.core.config.LegalEntityConfigurationProperties.Cursor;
import com.backbase.stream.compositions.legalentity.core.config.LegalEntityConfigurationProperties.Events;
import com.backbase.stream.compositions.legalentity.core.config.LegalEntityConfigurationProperties.ProductComposition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LegalEntityConfigurationPropertiesTest {

  @Test
  void test() {
    LegalEntityConfigurationProperties properties = new LegalEntityConfigurationProperties();
    properties.setIntegrationBaseUrl("https://legal-entity");

    Chains chains = new Chains();
    ProductComposition productComposition = new ProductComposition();
    productComposition.setBaseUrl("https://product-composition");
    productComposition.setEnabled(Boolean.TRUE);
    productComposition.setAsync(Boolean.TRUE);
    chains.setProductComposition(productComposition);

    Events events = new Events();
    events.setEnableCompleted(Boolean.TRUE);
    events.setEnableFailed(Boolean.TRUE);

    Cursor cursor = new Cursor();
    cursor.setBaseUrl("https://cursor");
    cursor.setEnabled(Boolean.TRUE);

    properties.setChains(chains);
    properties.setEvents(events);
    properties.setCursor(cursor);

    assertTrue(
        properties.getIntegrationBaseUrl().contains("legal-entity"), "Correct config spotted");
    assertTrue(
        properties.getChains().getProductComposition().getBaseUrl().contains("product-composition"),
        "Correct config spotted");
    assertTrue(properties.getEvents().getEnableCompleted());
    assertTrue(properties.getEvents().getEnableFailed());
    assertTrue(properties.getCursor().getEnabled());
    assertTrue(properties.getCursor().getBaseUrl().contains("cursor"), "Correct config spotted");
    assertTrue(properties.isCompletedEventEnabled());
    assertTrue(properties.isFailedEventEnabled());
    assertTrue(properties.isProductChainEnabled());
    assertTrue(properties.isProductChainAsync());
  }
}
