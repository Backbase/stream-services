package com.backbase.stream.config;

import com.backbase.buildingblocks.webclient.WebClientConstants;
import com.backbase.dbs.paymentorder.api.service.ApiClient;
import com.backbase.dbs.paymentorder.api.service.v2.PaymentOrdersApi;
import com.backbase.stream.PaymentOrderService;
import com.backbase.stream.PaymentOrderServiceImpl;
import com.backbase.stream.mappers.PaymentOrderTypeMapper;
import com.backbase.stream.paymentorder.PaymentOrderTask;
import com.backbase.stream.paymentorder.PaymentOrderTaskExecutor;
import com.backbase.stream.paymentorder.PaymentOrderUnitOfWorkExecutor;
import com.backbase.stream.paymentorder.repository.PaymentOrderUnitOfWorkRepository;
import com.backbase.stream.webclient.DbsWebClientConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;
import com.backbase.stream.worker.repository.impl.InMemoryReactiveUnitOfWorkRepository;

import java.text.DateFormat;

@EnableConfigurationProperties({
    BackbaseStreamConfigurationProperties.class,
    PaymentOrderWorkerConfigurationProperties.class
})
@AllArgsConstructor
@Configuration
@Import({DbsWebClientConfiguration.class})
public class PaymentOrderServiceConfiguration {

    private final PaymentOrderTypeMapper paymentOrderTypeMapper;


    @Bean
    public PaymentOrderTaskExecutor paymentOrderTaskExecutor(ApiClient paymentOrderApiClient) {
        PaymentOrdersApi paymentOrdersApi = new PaymentOrdersApi(paymentOrderApiClient);
        return new PaymentOrderTaskExecutor(paymentOrdersApi, paymentOrderTypeMapper);
    }

    @Bean
    public PaymentOrderUnitOfWorkExecutor paymentOrderUnitOfWorkExecutor(PaymentOrderTaskExecutor paymentOrderTaskExecutor,
                                                                        PaymentOrderUnitOfWorkRepository paymentOrderUnitOfWorkRepository,
                                                                        PaymentOrderWorkerConfigurationProperties paymentOrderWorkerConfigurationProperties) {

        return new PaymentOrderUnitOfWorkExecutor(paymentOrderUnitOfWorkRepository, paymentOrderTaskExecutor,
                paymentOrderWorkerConfigurationProperties);
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

    @Bean
    public ApiClient paymentOrderApiClient(ObjectMapper objectMapper,
                                           DateFormat dateFormat,
                                           @Qualifier(WebClientConstants.INTER_SERVICE_WEB_CLIENT_NAME) WebClient dbsWebClient,
                                           BackbaseStreamConfigurationProperties config) {
        ApiClient apiClient = new ApiClient(dbsWebClient, objectMapper, dateFormat);
        apiClient.setBasePath(config.getDbs().getPaymentOrderBaseUrl());
        return apiClient;
    }
}
