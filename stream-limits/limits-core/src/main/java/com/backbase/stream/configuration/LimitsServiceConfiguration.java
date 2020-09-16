package com.backbase.stream.configuration;

import com.backbase.dbs.limit.service.ApiClient;
import com.backbase.dbs.limit.service.api.LimitsApi;
import com.backbase.dbs.user.presentation.service.api.UsersApi;
import com.backbase.stream.config.BackbaseStreamConfigurationProperties;
import com.backbase.stream.limit.LimitsSaga;
import com.backbase.stream.limit.LimitsTask;
import com.backbase.stream.limit.LimitsUnitOfWorkExecutor;
import com.backbase.stream.limit.repository.LimitsUnitOfWorkRepository;
import com.backbase.stream.webclient.DbsWebClientConfiguration;
import com.backbase.stream.worker.repository.impl.InMemoryReactiveUnitOfWorkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

@EnableConfigurationProperties({
    BackbaseStreamConfigurationProperties.class,
    LimitsWorkerConfigurationProperties.class
})
@AllArgsConstructor
@Configuration
@Import({DbsWebClientConfiguration.class})
public class LimitsServiceConfiguration {

    @Bean
    public LimitsApi limitsApi(
        ObjectMapper objectMapper,
        DateFormat dateFormat,
        WebClient dbsWebClient,
        BackbaseStreamConfigurationProperties configurationProperties
    ) {
        ApiClient apiClient = new ApiClient(dbsWebClient, objectMapper, dateFormat).setBasePath(configurationProperties.getDbs().getLimitsManagerBaseUrl());
        return new LimitsApi(apiClient);
    }

    @Bean
    public UsersApi usersApi(
        ObjectMapper objectMapper,
        DateFormat dateFormat,
        WebClient dbsWebClient,
        BackbaseStreamConfigurationProperties configurationProperties
    ) {
        com.backbase.dbs.user.presentation.service.ApiClient apiClient = new com.backbase.dbs.user.presentation.service.ApiClient(dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(configurationProperties.getDbs().getUserManagerBaseUrl());
        return new UsersApi(apiClient);
    }

    @Bean
    public LimitsSaga limitsSaga(LimitsApi limitsApi) {
        return new LimitsSaga(limitsApi);
    }

    public static class InMemoryLimitsUnitOfWorkRepository extends InMemoryReactiveUnitOfWorkRepository<LimitsTask> implements LimitsUnitOfWorkRepository {
    }

    @Bean
    @ConditionalOnProperty(name = "backbase.stream.persistence", havingValue = "memory", matchIfMissing = true)
    public LimitsUnitOfWorkRepository limitsUnitOfWorkRepository() {
        return new InMemoryLimitsUnitOfWorkRepository();
    }

    @Bean
    public LimitsUnitOfWorkExecutor limitsUnitOfWorkExecutor(
        LimitsUnitOfWorkRepository repository, LimitsSaga saga, LimitsWorkerConfigurationProperties configurationProperties
    ) {
        return new LimitsUnitOfWorkExecutor(repository, saga, configurationProperties);
    }

}
