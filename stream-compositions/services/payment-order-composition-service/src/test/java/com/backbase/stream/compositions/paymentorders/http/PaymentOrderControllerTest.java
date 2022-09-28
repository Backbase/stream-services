package com.backbase.stream.compositions.paymentorders.http;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostResponse;
import com.backbase.dbs.paymentorder.api.service.v2.model.UpdateStatusPut;
import com.backbase.stream.PaymentOrderService;
import com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderIngestionResponse;
import com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPullIngestionRequest;
import com.backbase.stream.compositions.paymentorders.core.mapper.PaymentOrderMapper;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestResponse;
import com.backbase.stream.compositions.paymentorders.core.service.PaymentOrderIngestionService;
import com.backbase.stream.model.PaymentOrderIngestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class PaymentOrderControllerTest {

    PaymentOrderController paymentOrderController;

    @Mock
    PaymentOrderIngestionService paymentOrderIngestionService;

    @Mock
    PaymentOrderService paymentOrderService;

    @Mock
    PaymentOrderMapper paymentOrderMapper;

    @BeforeEach
    void setUp() {
        paymentOrderController = new PaymentOrderController(
                paymentOrderIngestionService,
                paymentOrderService,
                paymentOrderMapper);
    }

    @Test
    void testPullIngestion_Success() {

        Mono<PaymentOrderPullIngestionRequest> requestMono = Mono.just(
                new PaymentOrderPullIngestionRequest().withInternalUserId("internalUserId"));

        List<PaymentOrderPostResponse> newPaymentOrderResponse = new ArrayList<>();
        PaymentOrderPostResponse paymentOrderPostResponse = new PaymentOrderPostResponse().id("id");
        newPaymentOrderResponse.add(paymentOrderPostResponse);

        List<UpdateStatusPut> updatedPaymentOrderResponse = new ArrayList<>();
        List<String> deletePaymentOrderResponse = new ArrayList<>();

        doAnswer(invocation -> {

            return Mono.just(PaymentOrderIngestResponse.builder()
                            .paymentOrderIngestContext(new PaymentOrderIngestContext()
                                    .internalUserId("internalId")
                                    .newPaymentOrderResponse(newPaymentOrderResponse)
                                    .updatedPaymentOrderResponse(updatedPaymentOrderResponse)
                                    .deletePaymentOrderResponse(deletePaymentOrderResponse))
                    .build());
        }).when(paymentOrderIngestionService).ingestPull(any());

        ResponseEntity<PaymentOrderIngestionResponse> responseEntity = paymentOrderController.pullPaymentOrder(requestMono, null)
                .block();

        PaymentOrderIngestionResponse ingestionResponse = responseEntity.getBody();
        assertNotNull(ingestionResponse);
        assertNotNull(ingestionResponse.getNewPaymentOrder());
        assertNotNull(ingestionResponse.getUpdatedPaymentOrder());
        assertNotNull(ingestionResponse.getDeletedPaymentOrder());
    }

}