package com.backbase.stream.configuration;

import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backbase.stream.transaction.worker")
@Slf4j
@Data
public class TransactionWorkerConfigurationProperties extends StreamWorkerConfiguration {

    private boolean groupPerArrangementId;

    private boolean continueOnError;
}
