package com.backbase.stream.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

import com.backbase.dbs.arrangement.api.service.v2.ArrangementsApi;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItem;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItems;
import com.backbase.dbs.paymentorder.api.service.v2.PaymentOrdersApi;
import com.backbase.dbs.paymentorder.api.service.v2.model.GetPaymentOrderResponse;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostFilterResponse;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostResponse;
import com.backbase.stream.common.PaymentOrderBaseTest;
import com.backbase.stream.config.PaymentOrderTypeConfiguration;
import com.backbase.stream.config.PaymentOrderWorkerConfigurationProperties;
import com.backbase.stream.model.request.NewPaymentOrderIngestRequest;
import com.backbase.stream.model.request.PaymentOrderIngestRequest;
import com.backbase.stream.paymentorder.PaymentOrderTask;
import com.backbase.stream.paymentorder.PaymentOrderTaskExecutor;
import com.backbase.stream.paymentorder.PaymentOrderUnitOfWorkExecutor;
import com.backbase.stream.worker.model.UnitOfWork;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;
import java.math.BigDecimal;

import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

@ExtendWith(MockitoExtension.class)
public class PaymentOrderUnitOfWorkExecutorTest extends PaymentOrderBaseTest {

    @Mock
    private PaymentOrdersApi paymentOrdersApi;

    @Mock
    private ArrangementsApi arrangementsApi;

    @Mock
    private UnitOfWorkRepository<PaymentOrderTask, String> repository;

    private PaymentOrderTypeConfiguration paymentOrderTypeConfiguration = new PaymentOrderTypeConfiguration();

    private final PaymentOrderTaskExecutor streamTaskExecutor = new PaymentOrderTaskExecutor(paymentOrdersApi);

    private final PaymentOrderWorkerConfigurationProperties streamWorkerConfiguration = new PaymentOrderWorkerConfigurationProperties();

    //    @InjectMocks
    private PaymentOrderUnitOfWorkExecutor paymentOrderUnitOfWorkExecutor;

    @BeforeEach
    void setup() {
        List<String> pmtTypes = new ArrayList<>();
        pmtTypes.add("INTRA_PMT");
        paymentOrderTypeConfiguration.setTypes(pmtTypes);
        paymentOrderUnitOfWorkExecutor = new PaymentOrderUnitOfWorkExecutor(
                repository, streamTaskExecutor, streamWorkerConfiguration,
                paymentOrdersApi, arrangementsApi, paymentOrderTypeMapper,
                paymentOrderTypeConfiguration);
    }

    @Test
    void test_prepareUnitOfWork_paymentOrderIngestRequestList() {
        List<PaymentOrderIngestRequest> paymentOrderIngestRequestList = List.of(
                new NewPaymentOrderIngestRequest(paymentOrderPostRequest.get(0)),
                new NewPaymentOrderIngestRequest(paymentOrderPostRequest.get(1))
        );

        PaymentOrderPostResponse paymentOrderPostResponse = new PaymentOrderPostResponse()
                .id("po_post_resp_id")
                .putAdditionsItem("key", "val");

        lenient().when(paymentOrdersApi.postPaymentOrder(Mockito.any()))
                .thenReturn(Mono.just(paymentOrderPostResponse));

        AccountArrangementItem accountArrangementItem = new AccountArrangementItem()
                .id("arrangementId_1")
                .externalArrangementId("externalArrangementId_1");

        AccountArrangementItems accountArrangementItems = new AccountArrangementItems()
                .addArrangementElementsItem(accountArrangementItem);

        lenient().when(arrangementsApi.postFilter(Mockito.any()))
                .thenReturn(Mono.just(accountArrangementItems));

        StepVerifier.create(paymentOrderUnitOfWorkExecutor.prepareUnitOfWork(paymentOrderIngestRequestList))
                .assertNext(unitOfWork -> {
                    Assertions.assertTrue(unitOfWork.getUnitOfOWorkId().startsWith("payment-orders-mixed-"));
                    Assertions.assertEquals(UnitOfWork.State.NEW, unitOfWork.getState());
                    Assertions.assertEquals(1, unitOfWork.getStreamTasks().size());
                    Assertions.assertEquals(paymentOrderIngestRequestList.size(), unitOfWork.getStreamTasks().get(0).getData().size());
                })
                .verifyComplete();
    }

    @Test
    void test_prepareUnitOfWork_paymentOrderPostRequestFlux() {
        Flux<PaymentOrderPostRequest> paymentOrderPostRequestFlux = Flux.fromIterable(paymentOrderPostRequest);

        PaymentOrderPostResponse paymentOrderPostResponse = new PaymentOrderPostResponse()
                .id("po_post_resp_id")
                .putAdditionsItem("key", "val");

        lenient().when(paymentOrdersApi.postPaymentOrder(any()))
                .thenReturn(Mono.just(paymentOrderPostResponse));

        GetPaymentOrderResponse getPaymentOrderResponse = new GetPaymentOrderResponse()
                .id("arrangementId_1")
                .bankReferenceId("bankReferenceId_1");
        PaymentOrderPostFilterResponse paymentOrderPostFilterResponse = new PaymentOrderPostFilterResponse()
                .addPaymentOrdersItem(getPaymentOrderResponse)
                .totalElements(new BigDecimal(1));

        doReturn(Mono.just(paymentOrderPostFilterResponse)).when(paymentOrdersApi).postFilterPaymentOrders(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        AccountArrangementItem accountArrangementItem = new AccountArrangementItem()
                .id("arrangementId_1")
                .externalArrangementId("externalArrangementId_1");
        AccountArrangementItems accountArrangementItems = new AccountArrangementItems()
                .addArrangementElementsItem(accountArrangementItem);

        Mockito.when(arrangementsApi.postFilter(Mockito.any()))
                .thenReturn(Mono.just(accountArrangementItems));

        StepVerifier.create(paymentOrderUnitOfWorkExecutor.prepareUnitOfWork(paymentOrderPostRequestFlux))
                .assertNext(unitOfWork -> {
                    Assertions.assertTrue(unitOfWork.getUnitOfOWorkId().startsWith("payment-orders-mixed-"));
                    Assertions.assertEquals(UnitOfWork.State.NEW, unitOfWork.getState());
                    Assertions.assertEquals(1, unitOfWork.getStreamTasks().size());
                    Assertions.assertEquals(paymentOrderPostRequest.size(), unitOfWork.getStreamTasks().get(0).getData().size());
                })
                .verifyComplete();
    }

    @Test
    void test_prepareunitofwork_blankuserid() {

        paymentOrderUnitOfWorkExecutor = new PaymentOrderUnitOfWorkExecutor(
                repository, streamTaskExecutor, streamWorkerConfiguration,
                paymentOrdersApi, arrangementsApi, null, paymentOrderTypeConfiguration);

        paymentOrderPostRequest.get(0).setInternalUserId(StringUtils.EMPTY);
        paymentOrderPostRequest.get(1).setInternalUserId(null);

        Flux<PaymentOrderPostRequest> paymentOrderPostRequestFlux = Flux.fromIterable(paymentOrderPostRequest);

        AccountArrangementItem accountArrangementItem = new AccountArrangementItem()
                .id("arrangementId_1")
                .externalArrangementId("externalArrangementId_1");
        AccountArrangementItems accountArrangementItems = new AccountArrangementItems()
                .addArrangementElementsItem(accountArrangementItem);

        lenient().when(arrangementsApi.postFilter(Mockito.any()))
                .thenReturn(Mono.just(accountArrangementItems));

        GetPaymentOrderResponse getPaymentOrderResponseWithEmptyUserId = new GetPaymentOrderResponse()
                .id("arrangementId_1");

        PaymentOrderPostFilterResponse paymentOrderPostFilterResponse = new PaymentOrderPostFilterResponse()
                .addPaymentOrdersItem(getPaymentOrderResponseWithEmptyUserId)
                .totalElements(new BigDecimal(1));

        lenient().doReturn(Mono.just(paymentOrderPostFilterResponse)).when(paymentOrdersApi).postFilterPaymentOrders(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        StepVerifier
                .create(paymentOrderUnitOfWorkExecutor.prepareUnitOfWork(paymentOrderPostRequestFlux))
                .expectNextCount(0)
                .verifyComplete();
    }

}