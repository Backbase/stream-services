package com.backbase.stream.config;

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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
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
    public PaymentOrderService paymentOrderService(ApiClient paymentOrderApiClient,
                                                  PaymentOrderUnitOfWorkExecutor paymentOrderUnitOfWorkExecutor) {
//        PaymentOrdersApi paymentOrdersApi = new PaymentOrdersApi(paymentOrderApiClient);

//        return new PaymentOrderServiceImpl(paymentOrdersApi, paymentOrderUnitOfWorkExecutor);
        return new PaymentOrderServiceImpl(paymentOrderUnitOfWorkExecutor);
    }

    @Bean
    public ApiClient paymentOrderApiClient(ObjectMapper objectMapper,
                                           DateFormat dateFormat,
                                           WebClient dbsWebClient,
                                           BackbaseStreamConfigurationProperties config) {
        ApiClient apiClient = new ApiClient(dbsWebClient, objectMapper, dateFormat);
        // todo - make this configurable
        apiClient.setBasePath("http://localhost:8090/payment-order-service");
        return apiClient;
    }

    @Bean
    @Primary
    public Jackson2ObjectMapperBuilder customObjectMapper() {
        return new Jackson2ObjectMapperBuilder()
                // other configs are possible
                .modules(new JsonNullableModule(), new JavaTimeModule());
    }
}
