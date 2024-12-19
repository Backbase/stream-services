package com.backbase.stream.configuration;

import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backbase.stream.loans.worker")
@Slf4j
@Data
public class LoansWorkerConfigurationProperties extends StreamWorkerConfiguration {
    private boolean continueOnError;
}
