package com.backbase.stream.contacts.configuration;

import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backbase.stream.limits.worker")
@Slf4j
@Data
public class ContactsWorkerConfigurationProperties extends StreamWorkerConfiguration {
    private boolean continueOnError;
}
