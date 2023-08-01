package com.backbase.stream.configuration;

import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backbase.stream.limits.worker")
@Slf4j
@Data
public class LimitsWorkerConfigurationProperties extends StreamWorkerConfiguration {
    private boolean continueOnError;
    private boolean enabled = true;
}
