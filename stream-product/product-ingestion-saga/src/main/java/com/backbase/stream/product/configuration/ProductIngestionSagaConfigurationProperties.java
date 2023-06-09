package com.backbase.stream.product.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("backbase.stream.product.sink")
public class ProductIngestionSagaConfigurationProperties {

  /** Enable identity integration */
  private boolean identityEnabled = false;

  /** Number of Worker Threads to Unit Of Work Executors */
  private int unitOfWorkExecutors = 1;

  /** Number of Worker Threads for Tasks */
  private int taskExecutors = 3;
}
