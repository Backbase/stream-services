package com.backbase.stream;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Tests to verify the default configuration is set for SCS services.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class DefaultSpringCloudStreamPropertiesTest {

    @Autowired
    private RabbitAutoConfiguration rabbitAutoConfiguration;

    @Autowired
    private KafkaAutoConfiguration kafkaAutoConfiguration;

    /**
     * Test that the rabbitAutoConfiguration is created.
     */
    @Test
    public void rabbitAutoConfigurationShouldExist() {
        assertNotNull(rabbitAutoConfiguration);
    }

    /**
     * Test that the kafkaAutoConfiguration is not created.
     */
    @Test
    public void kafkaAutoConfigurationShouldNotExist() {
        assertNotNull(kafkaAutoConfiguration);
    }

}
