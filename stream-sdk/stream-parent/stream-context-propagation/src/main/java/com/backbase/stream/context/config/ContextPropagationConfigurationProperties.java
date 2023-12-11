package com.backbase.stream.context.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Data
@EnableConfigurationProperties
@ConfigurationProperties("backbase.stream.context")
public class ContextPropagationConfigurationProperties {

    public static final String TENANT_HEADER_NAME = "X-TID";

    /**
     * Header keys from the original request to forward to DBS Service calls.
     */
    private List<String> headersToForward = List.of(TENANT_HEADER_NAME);

}
