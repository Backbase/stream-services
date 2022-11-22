package com.backbase.stream.configuration;

import com.backbase.dbs.limit.api.service.v2.LimitsServiceApi;
import com.backbase.stream.limit.LimitsSaga;
import com.backbase.stream.limit.LimitsTask;
import com.backbase.stream.limit.LimitsUnitOfWorkExecutor;
import com.backbase.stream.limit.repository.LimitsUnitOfWorkRepository;
import com.backbase.stream.worker.repository.impl.InMemoryReactiveUnitOfWorkRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({
    LimitsWorkerConfigurationProperties.class
})
@AllArgsConstructor
@Configuration
public class LimitsServiceConfiguration {

    @Bean
    public LimitsSaga limitsSaga(LimitsServiceApi limitsServiceApi) {
        return new LimitsSaga(limitsServiceApi);
    }

    public static class InMemoryLimitsUnitOfWorkRepository extends
        InMemoryReactiveUnitOfWorkRepository<LimitsTask> implements LimitsUnitOfWorkRepository {

    }

    @Bean
    @ConditionalOnProperty(name = "backbase.stream.persistence", havingValue = "memory", matchIfMissing = true)
    public LimitsUnitOfWorkRepository limitsUnitOfWorkRepository() {
        return new InMemoryLimitsUnitOfWorkRepository();
    }

    @Bean
    public LimitsUnitOfWorkExecutor limitsUnitOfWorkExecutor(
        LimitsUnitOfWorkRepository repository, LimitsSaga saga,
        LimitsWorkerConfigurationProperties configurationProperties
    ) {
        return new LimitsUnitOfWorkExecutor(repository, saga, configurationProperties);
    }

}
