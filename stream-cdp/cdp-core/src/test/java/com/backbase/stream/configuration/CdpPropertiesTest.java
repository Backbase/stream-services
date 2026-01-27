package com.backbase.stream.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CdpPropertiesTest {

    @Test
    void testProperties() {
        CdpProperties properties = new CdpProperties(true, "testCategory");

        assertTrue(properties.enabled());
        assertEquals("testCategory", properties.defaultCustomerCategory());
    }
}