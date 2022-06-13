package com.backbase.stream.webclient.configuration;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.MultiValueMap;

import java.util.List;

@Data
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.client")
public class DbsWebClientConfigurationProperties {

    /**
     * Additional headers sent in each request to DBS Service API's.
     * This is used to set the X-TID Header for setting the right tenant in a multi-tenant environment
     */
    private MultiValueMap<String, String> additionalHeaders;

    /**
     * Header keys from the original request to forward to DBS
     */
    private List<String> headersToForward = asList("X-TID");

}
