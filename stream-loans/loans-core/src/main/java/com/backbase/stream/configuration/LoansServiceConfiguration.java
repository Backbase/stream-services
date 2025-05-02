package com.backbase.stream.configuration;

import com.backbase.loan.inbound.api.service.v2.LoansApi;
import com.backbase.stream.loan.LoansSaga;
import com.backbase.stream.loan.LoansTask;
import com.backbase.stream.loan.LoansUnitOfWorkExecutor;
import com.backbase.stream.loan.repository.LoansUnitOfWorkRepository;
import com.backbase.stream.worker.repository.impl.InMemoryReactiveUnitOfWorkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({
    LoansWorkerConfigurationProperties.class
})
@RequiredArgsConstructor
@Configuration
public class LoansServiceConfiguration {

    @Bean
    public LoansSaga loansSaga(LoansApi loansApi) {
        return new LoansSaga(loansApi);
    }

    public static class InMemoryLoansUnitOfWorkRepository extends
        InMemoryReactiveUnitOfWorkRepository<LoansTask> implements LoansUnitOfWorkRepository {

    }

    @Bean
    @ConditionalOnProperty(name = "backbase.stream.persistence", havingValue = "memory", matchIfMissing = true)
    public LoansUnitOfWorkRepository loansUnitOfWorkRepository() {
        return new InMemoryLoansUnitOfWorkRepository();
    }

    @Bean
    public LoansUnitOfWorkExecutor loansUnitOfWorkExecutor(
        LoansUnitOfWorkRepository repository, LoansSaga saga,
        LoansWorkerConfigurationProperties configurationProperties
    ) {
        return new LoansUnitOfWorkExecutor(repository, saga, configurationProperties);
    }

}
