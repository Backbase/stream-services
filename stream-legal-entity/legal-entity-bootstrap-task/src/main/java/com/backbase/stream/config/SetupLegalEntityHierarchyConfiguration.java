package com.backbase.stream.config;

import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.legalentity.model.LegalEntity;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;


@EnableTask
@Configuration
@AllArgsConstructor
@Slf4j
@EnableConfigurationProperties(BootstrapConfigurationProperties.class)
public class SetupLegalEntityHierarchyConfiguration {

    private final LegalEntitySaga legalEntitySaga;
    private final BootstrapConfigurationProperties bootstrapConfigurationProperties;

    @Bean
    public CommandLineRunner commandLineRunner() {
        return this::run;
    }

    private void run(String... args) {
        LegalEntity legalEntity = bootstrapConfigurationProperties.getLegalEntity();
        log.info("Bootstrapping Root Legal Entity Structure: {}", legalEntity.getName());

        List<LegalEntity> aggregates = Collections.singletonList(bootstrapConfigurationProperties.getLegalEntity());

        Flux.fromIterable(aggregates)
            .map(LegalEntityTask::new)
            .flatMap(legalEntitySaga::executeTask)
            .collectList()
            .block();

        log.info("Finished bootstrapping Legal Entity Structure");
    }
}
