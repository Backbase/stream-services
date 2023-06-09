package com.backbase.stream.webclient.configuration;

import static java.util.Arrays.asList;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.MultiValueMap;

@Data
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.client")
public class DbsWebClientConfigurationProperties {

  /**
   * Additional headers sent in each request to DBS Service APIs. The values defined here will be
   * eventually overridden by the header values configured in 'headersToForward' property. For
   * instance, this is used to set the X-TID Header in single execution tasks when setting the right
   * tenant context in a multi-tenant environment.
   */
  private MultiValueMap<String, String> additionalHeaders;

  /** Header keys from the original request to forward to DBS Service calls. */
  private List<String> headersToForward = asList("X-TID");
}
