package com.backbase.stream.configuration;

import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.LegalEntityUnitOfWorkExecutor;
import com.backbase.stream.contact.ContactsSaga;
import com.backbase.stream.legalentity.repository.LegalEntityUnitOfWorkRepository;
import com.backbase.stream.limit.LimitsSaga;
import com.backbase.stream.product.BatchProductIngestionSaga;
import com.backbase.stream.product.ProductIngestionSaga;
import com.backbase.stream.product.ProductIngestionSagaConfiguration;
import com.backbase.stream.product.configuration.ProductConfiguration;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.LegalEntityService;
import com.backbase.stream.service.UserProfileService;
import com.backbase.stream.service.UserService;
import com.backbase.stream.worker.repository.impl.InMemoryReactiveUnitOfWorkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
    ProductConfiguration.class,
    AccessControlConfiguration.class,
    ProductIngestionSagaConfiguration.class,
    LimitsServiceConfiguration.class,
        ContactsServiceConfiguration.class
})
@EnableConfigurationProperties(
    {LegalEntitySagaConfigurationProperties.class}
)
public class LegalEntitySagaConfiguration {

    @Bean
    public LegalEntitySaga reactiveLegalEntitySaga(LegalEntityService legalEntityService,
        UserService userService,
        UserProfileService userProfileService,
        AccessGroupService accessGroupService,
        ProductIngestionSaga productIngestionSaga,
        BatchProductIngestionSaga batchProductIngestionSaga,
        LimitsSaga limitsSaga,
        ContactsSaga contactsSaga,
        LegalEntitySagaConfigurationProperties sinkConfigurationProperties,
        ObjectMapper objectMapper) {
        return new LegalEntitySaga(
            legalEntityService,
            userService,
            userProfileService,
            accessGroupService,
            productIngestionSaga,
            batchProductIngestionSaga, limitsSaga,
                contactsSaga,
                sinkConfigurationProperties
        );
    }

    @Bean
    @ConditionalOnProperty(name = "backbase.stream.persistence", havingValue = "memory", matchIfMissing = true)
    public LegalEntityUnitOfWorkRepository legalEntityInMemoryUnitOfWorkRepository() {
        return new LegalEntityInMemoryUnitOfWorkRepository();
    }

    @Bean
    public LegalEntityUnitOfWorkExecutor legalEntityUnitOfWorkExecutor(
        LegalEntityUnitOfWorkRepository legalEntityUnitOfWorkRepository,
        LegalEntitySaga legalEntitySaga,
        LegalEntitySagaConfigurationProperties configProperties) {

        return new LegalEntityUnitOfWorkExecutor(legalEntityUnitOfWorkRepository, legalEntitySaga, configProperties);
    }

    public static class LegalEntityInMemoryUnitOfWorkRepository extends
        InMemoryReactiveUnitOfWorkRepository<LegalEntityTask> implements LegalEntityUnitOfWorkRepository {

    }

}
