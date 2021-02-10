package com.backbase.stream.audit.configuration;

import com.backbase.dbs.audit.api.service.ApiClient;
import com.backbase.dbs.audit.api.service.v2.AuditServiceApi;
import com.backbase.stream.audit.AuditMessagesTask;
import com.backbase.stream.audit.AuditTaskExecutor;
import com.backbase.stream.audit.AuditUnitOfWorkExecutor;
import com.backbase.stream.audit.repository.AuditMessageTaskRepository;
import com.backbase.stream.webclient.DbsWebClientConfiguration;
import com.backbase.stream.worker.model.UnitOfWork;
import com.backbase.stream.worker.repository.impl.InMemoryReactiveUnitOfWorkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Configuration
@EnableConfigurationProperties(AuditConfigurationProperties.class)
@Import({
    DbsWebClientConfiguration.class
})
@Slf4j
public class AuditConfiguration {

    private Flux<UnitOfWork<AuditMessagesTask>> scheduler;

    @Bean
    public AuditServiceApi auditMessagesApi(WebClient dbsWebClient,
                                             ObjectMapper mapper,
                                             DateFormat format,
                                             AuditConfigurationProperties auditConfigurationProperties) {
        ApiClient apiClient = new ApiClient(dbsWebClient, mapper, format);
        apiClient.setBasePath(auditConfigurationProperties.getAuditPresentationBaseUrl());
        return new AuditServiceApi(apiClient);
    }

    @Bean
    public AuditTaskExecutor auditTaskExecutor(AuditServiceApi auditMessagesApi) {
        return new AuditTaskExecutor(auditMessagesApi);
    }

    @Bean
    public AuditUnitOfWorkExecutor auditUnitOfWorkExecutor(AuditTaskExecutor auditTaskExecutor,
                                                           AuditMessageTaskRepository repository,
                                                           AuditConfigurationProperties auditConfigurationProperties) {
        AuditUnitOfWorkExecutor auditUnitOfWorkExecutor = new AuditUnitOfWorkExecutor(repository, auditTaskExecutor,
            auditConfigurationProperties);
        scheduler = auditUnitOfWorkExecutor.getScheduler();
        return auditUnitOfWorkExecutor;
    }

    @Bean
    @ConditionalOnProperty(value = "backbase.stream.persistence", havingValue = "memory", matchIfMissing = true)
    public AuditMessageTaskRepository inMemoryReactiveUnitOfWorkRepository() {
        return new LegalEntityInMemoryUnitOfWorkRepository();
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Starting Unit Of Work Scheduler");
        scheduler.subscribe();
    }


    public static class LegalEntityInMemoryUnitOfWorkRepository extends
        InMemoryReactiveUnitOfWorkRepository<AuditMessagesTask> implements AuditMessageTaskRepository {

    }

}
