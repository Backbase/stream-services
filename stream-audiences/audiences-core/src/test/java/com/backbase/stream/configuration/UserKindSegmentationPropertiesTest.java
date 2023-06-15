package com.backbase.stream.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UserKindSegmentationPropertiesTest {

    @Test
    void testProperties() {
        UserKindSegmentationProperties properties = new UserKindSegmentationProperties(true, "testCategory");

        assertTrue(properties.enabled());
        assertEquals("testCategory", properties.defaultCustomerCategory());
    }
}