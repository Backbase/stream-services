package com.backbase.stream.configuration;

import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("backbase.stream.legalentity.serviceagreement.sink")
@Data
@NoArgsConstructor
public class UpdatedServiceAgreementSagaConfigurationProperties extends StreamWorkerConfiguration {

}
