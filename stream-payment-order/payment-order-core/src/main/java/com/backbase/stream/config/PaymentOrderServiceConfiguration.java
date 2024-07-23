package com.backbase.stream.config;

import com.backbase.dbs.arrangement.api.service.v2.ArrangementsApi;
import com.backbase.dbs.paymentorder.api.service.v2.PaymentOrdersApi;
import com.backbase.stream.PaymentOrderService;
import com.backbase.stream.PaymentOrderServiceImpl;
import com.backbase.stream.mappers.PaymentOrderTypeMapper;
import com.backbase.stream.paymentorder.PaymentOrderTask;
import com.backbase.stream.paymentorder.PaymentOrderTaskExecutor;
import com.backbase.stream.paymentorder.PaymentOrderUnitOfWorkExecutor;
import com.backbase.stream.paymentorder.repository.PaymentOrderUnitOfWorkRepository;
import com.backbase.stream.worker.repository.impl.InMemoryReactiveUnitOfWorkRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({
    PaymentOrderWorkerConfigurationProperties.class,
    PaymentOrderTypeConfiguration.class
})
@AllArgsConstructor
@Configuration
public class PaymentOrderServiceConfiguration {

    private final PaymentOrderTypeMapper paymentOrderTypeMapper;
    private final PaymentOrderTypeConfiguration paymentOrderTypeConfiguration;

    @Bean
    public PaymentOrderTaskExecutor paymentOrderTaskExecutor(PaymentOrdersApi paymentOrdersApi) {
        return new PaymentOrderTaskExecutor(paymentOrdersApi);
    }

    @Bean
    public PaymentOrderUnitOfWorkExecutor paymentOrderUnitOfWorkExecutor(
        PaymentOrderTaskExecutor paymentOrderTaskExecutor,
        PaymentOrderUnitOfWorkRepository paymentOrderUnitOfWorkRepository,
        PaymentOrderWorkerConfigurationProperties paymentOrderWorkerConfigurationProperties,
        PaymentOrdersApi paymentOrdersApi,
            ArrangementsApi arrangementsApi) {

        return new PaymentOrderUnitOfWorkExecutor(paymentOrderUnitOfWorkRepository, paymentOrderTaskExecutor,
                paymentOrderWorkerConfigurationProperties, paymentOrdersApi, arrangementsApi, paymentOrderTypeMapper,
                paymentOrderTypeConfiguration);
    }

    @Bean
    @ConditionalOnProperty(name = "backbase.stream.persistence", havingValue = "memory", matchIfMissing = true)
    public PaymentOrderUnitOfWorkRepository paymentOrderUnitOfWorkRepository() {
        return new InMemoryPaymentOrderUnitOfWorkRepository();
    }

    public static class InMemoryPaymentOrderUnitOfWorkRepository extends
        InMemoryReactiveUnitOfWorkRepository<PaymentOrderTask> implements PaymentOrderUnitOfWorkRepository {

    }

    @Bean
    public PaymentOrderService paymentOrderService(PaymentOrderUnitOfWorkExecutor paymentOrderUnitOfWorkExecutor) {
        return new PaymentOrderServiceImpl(paymentOrderUnitOfWorkExecutor);
    }
}
