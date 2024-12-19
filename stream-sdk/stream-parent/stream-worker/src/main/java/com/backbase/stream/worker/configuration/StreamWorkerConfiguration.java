package com.backbase.stream.worker.configuration;

import java.time.Duration;
import lombok.Data;

@Data
public abstract class StreamWorkerConfiguration {

  private int taskExecutors = 1;
  private int maxRetries = 3;
  private int bufferSize = 10;

  private Duration bufferMaxTime = Duration.ofMillis(100);
  private Duration retryDuration = Duration.ofMinutes(1);

  private int rateLimit = -1;
}
