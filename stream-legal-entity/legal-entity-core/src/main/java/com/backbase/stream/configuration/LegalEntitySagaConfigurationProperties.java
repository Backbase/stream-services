package com.backbase.stream.configuration;

import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;
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
    private BatchProductGroupTask.IngestionMode ingestionMode = BatchProductGroupTask.IngestionMode.REPLACE;

    public BatchProductGroupTask.IngestionMode getIngestionMode() {
        return ingestionMode;
    }
}
