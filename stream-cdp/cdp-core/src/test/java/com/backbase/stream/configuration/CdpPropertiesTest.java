package com.backbase.stream.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CdpPropertiesTest {

    @Test
    void testProperties() {
        CdpProperties properties = new CdpProperties(true, "testCategory");

        assertThat(properties.enabled()).isTrue();
        assertThat(properties.defaultCustomerCategory()).isEqualTo("testCategory");
    }
}
