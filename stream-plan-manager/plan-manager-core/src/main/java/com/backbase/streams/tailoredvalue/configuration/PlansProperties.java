package com.backbase.streams.tailoredvalue.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backbase.stream.plans")
@Getter
@Setter
public class PlansProperties {
  private boolean enabled;
}
