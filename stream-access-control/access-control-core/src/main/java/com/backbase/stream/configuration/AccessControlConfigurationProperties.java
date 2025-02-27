package com.backbase.stream.configuration;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties("backbase.stream.access.control")
public class AccessControlConfigurationProperties {

    @Min(1)
    private int userContextPageSize = 10;
}
