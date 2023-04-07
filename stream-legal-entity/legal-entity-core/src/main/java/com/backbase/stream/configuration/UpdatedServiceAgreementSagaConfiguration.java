package com.backbase.stream.configuration;

import com.backbase.stream.UpdatedServiceAgreementSaga;
import com.backbase.stream.UpdatedServiceAgreementTask;
import com.backbase.stream.UpdatedServiceAgreementUnitOfWorkExecutor;
import com.backbase.stream.legalentity.repository.UpdatedServiceAgreementUnitOfWorkRepository;
import com.backbase.stream.product.service.ArrangementService;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.worker.repository.impl.InMemoryReactiveUnitOfWorkRepository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({AccessControlConfiguration.class})
@EnableConfigurationProperties({UpdatedServiceAgreementSagaConfigurationProperties.class})
public class UpdatedServiceAgreementSagaConfiguration {

    @Bean
    public UpdatedServiceAgreementSaga reactiveUpdatedServiceAgreementSaga(
            AccessGroupService accessGroupService, ArrangementService arrangementService) {
        return new UpdatedServiceAgreementSaga(accessGroupService, arrangementService);
    }

    @Bean
    @ConditionalOnProperty(
            name = "backbase.stream.persistence",
            havingValue = "memory",
            matchIfMissing = true)
    public UpdatedServiceAgreementUnitOfWorkRepository
            updatedServiceAgreementInMemoryUnitOfWorkRepository() {
        return new UpdatedServiceAgreementInMemoryUnitOfWorkRepository();
    }

    @Bean
    public UpdatedServiceAgreementUnitOfWorkExecutor updatedServiceAgreementUnitOfWorkExecutor(
            UpdatedServiceAgreementUnitOfWorkRepository updatedServiceAgreementUnitOfWorkRepository,
            UpdatedServiceAgreementSaga updatedServiceAgreementSaga,
            UpdatedServiceAgreementSagaConfigurationProperties configProperties) {

        return new UpdatedServiceAgreementUnitOfWorkExecutor(
                updatedServiceAgreementUnitOfWorkRepository,
                updatedServiceAgreementSaga,
                configProperties);
    }

    public static class UpdatedServiceAgreementInMemoryUnitOfWorkRepository
            extends InMemoryReactiveUnitOfWorkRepository<UpdatedServiceAgreementTask>
            implements UpdatedServiceAgreementUnitOfWorkRepository {}
}
