package com.backbase.stream.configuration;

import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backbase.stream.contact.worker")
@Slf4j
@Getter
@Setter
public class ContactsWorkerConfigurationProperties extends StreamWorkerConfiguration {

  private boolean continueOnError;
}
