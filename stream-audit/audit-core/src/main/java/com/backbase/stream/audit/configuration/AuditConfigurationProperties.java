package com.backbase.stream.audit.configuration;

import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("backbase.stream.audit")
@Data
@NoArgsConstructor
public class AuditConfigurationProperties extends StreamWorkerConfiguration {

    @Value("${backbase.stream.dbs.audit-presentation-base-url}")
    private String auditPresentationBaseUrl;


}
