package com.backbase.stream.config;

import com.backbase.stream.ApprovalSaga;
import com.backbase.stream.ApprovalTask;
import com.backbase.stream.approval.model.Approval;
import com.backbase.stream.worker.model.StreamTask;
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

    private final ApprovalSaga approvalSaga;
    private final BootstrapConfigurationProperties bootstrapConfigurationProperties;

    @Bean
    public CommandLineRunner commandLineRunner() {
        return this::run;
    }

    private void run(String... args) {
        log.debug("Approval: {}", bootstrapConfigurationProperties.getApproval());
        Approval approval = bootstrapConfigurationProperties.getApproval();
        if (approval == null) {
            log.error("Failed to load Approval Structure");
            System.exit(1);
        } else {
            log.info("Bootstrapping Root Approval Structure");
            List<Approval> aggregates = Collections.singletonList(bootstrapConfigurationProperties.getApproval());

            Flux.fromIterable(aggregates)
                .map(ApprovalTask::new)
                .flatMap(approvalSaga::executeTask)
                .doOnNext(StreamTask::logSummary)
                .collectList()
                .block();
            log.info("Finished bootstrapping Legal Entity Structure");
            System.exit(0);
        }
    }
}
