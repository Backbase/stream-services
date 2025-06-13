package com.backbase.stream.configuration;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
@ConfigurationProperties("backbase.stream.access.control")
@Validated
public class AccessControlConfigurationProperties {

    @Min(1)
    private int userContextPageSize = 10;

    @Min(1)
    private int concurrency = 1;
}
