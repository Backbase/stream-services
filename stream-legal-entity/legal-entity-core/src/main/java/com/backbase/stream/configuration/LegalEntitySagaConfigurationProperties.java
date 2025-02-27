package com.backbase.stream.configuration;

import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("backbase.stream.legalentity.sink")
@Data
@NoArgsConstructor
public class LegalEntitySagaConfigurationProperties extends StreamWorkerConfiguration {

    /**
     * Enable identity integration
     */
    private boolean useIdentityIntegration = true;

    /**
     * Enable User Profile
     */
    private boolean userProfileEnabled = false;

    /**
     * Enable service agreement update
     */
    private boolean serviceAgreementUpdateEnabled = false;

    /**
     * List of service agreement purposes
     */
    private Set<String> serviceAgreementPurposes;

}
