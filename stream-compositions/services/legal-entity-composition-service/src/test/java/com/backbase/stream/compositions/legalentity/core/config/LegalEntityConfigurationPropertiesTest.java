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

        Chains chains = new Chains();
        ProductComposition productComposition = new ProductComposition();
        productComposition.setEnabled(Boolean.TRUE);
        productComposition.setAsync(Boolean.TRUE);
        chains.setProductComposition(productComposition);

        Events events = new Events();
        events.setEnableCompleted(Boolean.TRUE);
        events.setEnableFailed(Boolean.TRUE);

        Cursor cursor = new Cursor();
        cursor.setEnabled(Boolean.TRUE);

        properties.setChains(chains);
        properties.setEvents(events);
        properties.setCursor(cursor);

        assertTrue(properties.getEvents().getEnableCompleted());
        assertTrue(properties.getEvents().getEnableFailed());
        assertTrue(properties.getCursor().getEnabled());
        assertTrue(properties.isCompletedEventEnabled());
        assertTrue(properties.isFailedEventEnabled());
        assertTrue(properties.isProductChainEnabled());
        assertTrue(properties.isProductChainAsync());
    }
}
