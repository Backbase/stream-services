package com.backbase.stream.context.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Data
@EnableConfigurationProperties
@ConfigurationProperties("backbase.stream.context")
public class ContextPropagationConfigurationProperties {

    public static final String TENANT_HTTP_HEADER_NAME = "X-TID";
    public static final String TENANT_EVENT_HEADER_NAME = "bbTenantId";

    /**
     * Header keys from the original request to forward to DBS Service calls.
     */
    private List<String> headersToForward = List.of(TENANT_HTTP_HEADER_NAME);

}
