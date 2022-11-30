package com.backbase.stream.compositions.paymentorders.http;

import java.util.List;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.backbase.stream.PaymentOrderService;
import com.backbase.stream.compositions.paymentorder.api.PaymentOrderCompositionApi;
import com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderIngestionResponse;
import com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPullIngestionRequest;
import com.backbase.stream.compositions.paymentorders.core.mapper.PaymentOrderMapper;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestResponse;
import com.backbase.stream.compositions.paymentorders.core.service.PaymentOrderIngestionService;
import com.backbase.stream.model.response.PaymentOrderIngestDbsResponse;

import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
public class PaymentOrderController implements PaymentOrderCompositionApi {

    PaymentOrderIngestionService paymentOrderIngestionService;

    PaymentOrderService paymentOrderService;

    PaymentOrderMapper paymentOrderMapper;

    @Override
    public Mono<ResponseEntity<PaymentOrderIngestionResponse>> pullPaymentOrder(
            @ApiParam(value = "Pull Ingestion Request"  )
            @Valid
            @RequestBody(required = false) Mono<PaymentOrderPullIngestionRequest> paymentOrderPullIngestionRequest,
            ServerWebExchange exchange) {

        return paymentOrderPullIngestionRequest
                .map(paymentOrderMapper::mapPullRequest)
                .flatMap(paymentOrderIngestionService::ingestPull)
                .map(this::mapIngestionToResponse);
    }

    /**
     * Builds ingestion response for API endpoint.
     *
     * @param response PaymentOrderIngestResponse
     * @return IngestionResponse
     */
    private ResponseEntity<PaymentOrderIngestionResponse> mapIngestionToResponse(PaymentOrderIngestResponse response) {
        List<PaymentOrderIngestDbsResponse> paymentOrderIngestDbsResponses = response.getPaymentOrderIngestDbsResponses();
        return new ResponseEntity<>(paymentOrderMapper.mapPaymentOrderIngestionResponse(paymentOrderIngestDbsResponses), HttpStatus.CREATED);
    }
}
