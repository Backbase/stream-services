package com.backbase.stream.config;

import com.backbase.stream.ApprovalSaga;
import com.backbase.stream.ApprovalTask;
import com.backbase.stream.approval.model.Approval;
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
public class SetupApprovalsHierarchyConfiguration {

  private final ApprovalSaga approvalSaga;
  private final BootstrapConfigurationProperties bootstrapConfigurationProperties;

  @Bean
  public CommandLineRunner commandLineRunner() {
    return this::run;
  }

  private void run(String... args) {
    List<Approval> approvals = bootstrapConfigurationProperties.getApprovals();
    log.debug("Approvals: {}", approvals);
    log.info("Bootstrapping Root Approvals Structure");

    Flux.fromIterable(bootstrapConfigurationProperties.getApprovals())
        .map(ApprovalTask::new)
        .flatMap(approvalSaga::executeTask)
        .collectList()
        .block();

    log.info("Finished bootstrapping Approvals Structure");
  }
}
