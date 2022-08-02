package com.backbase.stream.config;

import com.backbase.dbs.paymentorder.api.service.ApiClient;
import com.backbase.dbs.paymentorder.api.service.v2.PaymentOrdersApi;
import com.backbase.stream.PaymentOrderService;
import com.backbase.stream.PaymentOrderServiceImpl;
import com.backbase.stream.paymentorder.PaymentOrderTask;
import com.backbase.stream.paymentorder.PaymentOrderTaskExecutor;
import com.backbase.stream.paymentorder.PaymentOrderUnitOfWorkExecutor;
import com.backbase.stream.paymentorder.repository.PaymentOrderUnitOfWorkRepository;
import com.backbase.stream.webclient.DbsWebClientConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
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


    @Bean
    public PaymentOrderTaskExecutor paymentOrderTaskExecutor(ApiClient paymentOrderApiClient) {
        PaymentOrdersApi paymentOrdersApi = new PaymentOrdersApi(paymentOrderApiClient);
        return new PaymentOrderTaskExecutor(paymentOrdersApi);
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
    public PaymentOrderService paymentOrderService(ApiClient paymentOrderApiClient,
                                                  PaymentOrderUnitOfWorkExecutor paymentOrderUnitOfWorkExecutor) {
        PaymentOrdersApi paymentOrdersApi = new PaymentOrdersApi(paymentOrderApiClient);

        return new PaymentOrderServiceImpl(paymentOrdersApi, paymentOrderUnitOfWorkExecutor);
    }

    @Bean
    public ApiClient paymentOrderApiClient(ObjectMapper objectMapper,
                                           DateFormat dateFormat,
                                           WebClient dbsWebClient,
                                           BackbaseStreamConfigurationProperties config) {
        ApiClient apiClient = new ApiClient(dbsWebClient, objectMapper, dateFormat);
        System.out.println("connecting to DBS with : " + config.getDbs().getPaymentOrderBaseUrl());
        apiClient.setBasePath("http://localhost:8090/payment-order-service");
        return apiClient;
    }
}
