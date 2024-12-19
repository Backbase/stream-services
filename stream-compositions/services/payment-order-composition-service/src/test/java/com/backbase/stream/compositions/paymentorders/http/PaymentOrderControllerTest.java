package com.backbase.stream.compositions.paymentorders.http;

import com.backbase.dbs.paymentorder.api.service.v3.model.PaymentOrderPostResponse;
import com.backbase.dbs.paymentorder.api.service.v3.model.PaymentOrderPutResponse;
import com.backbase.dbs.paymentorder.api.service.v3.model.UpdateStatusPut;
import com.backbase.stream.PaymentOrderService;
import com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderIngestionResponse;
import com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPullIngestionRequest;
import com.backbase.stream.compositions.paymentorders.core.mapper.PaymentOrderMapper;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestResponse;
import com.backbase.stream.compositions.paymentorders.core.service.PaymentOrderIngestionService;
import com.backbase.stream.model.response.DeletePaymentOrderIngestDbsResponse;
import com.backbase.stream.model.response.NewPaymentOrderIngestDbsResponse;
import com.backbase.stream.model.response.PaymentOrderIngestDbsResponse;
import com.backbase.stream.model.response.UpdatePaymentOrderIngestDbsResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
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

    PaymentOrderMapper paymentOrderMapper = Mappers.getMapper(PaymentOrderMapper.class);

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
                new PaymentOrderPullIngestionRequest().internalUserId("internalUserId"));

        List<PaymentOrderIngestDbsResponse> paymentOrderIngestDbsResponses = new ArrayList<>();
        paymentOrderIngestDbsResponses.add(new NewPaymentOrderIngestDbsResponse(new PaymentOrderPostResponse()));
        paymentOrderIngestDbsResponses.add(new UpdatePaymentOrderIngestDbsResponse(new PaymentOrderPutResponse()));
        paymentOrderIngestDbsResponses.add(new DeletePaymentOrderIngestDbsResponse("paymentOrderId"));

        doAnswer(invocation -> {

            return Mono.just(
                PaymentOrderIngestResponse.builder()
                    .memberNumber("memberNumber")
                    .paymentOrderIngestDbsResponses(paymentOrderIngestDbsResponses)
                    .build()
            );
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