package com.backbase.stream.worker.configuration;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
public abstract class StreamWorkerConfiguration {

    private int workerUnitExecutors = 1;

    private int taskExecutors = 1 ;

    private int maxRetries = 3;

    private int bufferSize = 10;

    private Duration bufferMaxTime = Duration.ofMillis(100);

    private Duration schedulerIntervalDuration = Duration.ofSeconds(5);

    private Duration retryDuration = Duration.ofMinutes(1);

    private Duration delayBetweenTasks = Duration.ZERO;

    private int rateLimit = -1;

}
