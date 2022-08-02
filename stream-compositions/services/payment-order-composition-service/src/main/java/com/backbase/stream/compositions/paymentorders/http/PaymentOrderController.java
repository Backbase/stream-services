package com.backbase.stream.compositions.paymentorders.http;

import com.backbase.stream.PaymentOrderService;
import com.backbase.stream.compositions.paymentorder.api.PaymentOrderCompositionApi;
import com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderIngestionResponse;
import com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPullIngestionRequest;
import com.backbase.stream.compositions.paymentorders.core.mapper.PaymentOrderMapper;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestPullRequest;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestResponse;
import com.backbase.stream.compositions.paymentorders.core.service.PaymentOrderIngestionService;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@Slf4j
public class PaymentOrderController implements PaymentOrderCompositionApi {

    PaymentOrderIngestionService paymentOrderIngestionService;

    PaymentOrderService paymentOrderService;

    PaymentOrderMapper paymentOrderMapper;

    @RequestMapping(value = "/service-api/v1/test",
            produces = { "application/json" },
            method = RequestMethod.GET)
    public String getTest() {

        return "test response;";
    }

    @Override
    public Mono<ResponseEntity<PaymentOrderIngestionResponse>> pullPaymentOrder(
            @ApiParam(value = "Pull Ingestion Request"  )
            @Valid
            @RequestBody(required = false) Mono<PaymentOrderPullIngestionRequest> paymentOrderPullIngestionRequest,
            ServerWebExchange exchange) {

        System.out.println("triggering payment ingestion");

        return paymentOrderPullIngestionRequest
                .map(this::buildPullRequest)
                .flatMap(paymentOrderIngestionService::ingestPull)
                .map(this::mapIngestionToResponse);
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PullIngestionRequest
     * @return ProductIngestPullRequest
     */
    private PaymentOrderIngestPullRequest buildPullRequest(PaymentOrderPullIngestionRequest request) {
        return PaymentOrderIngestPullRequest
                .builder()
                .arrangementId(request.getArrangementId())
                .legalEntityInternalId(request.getLegalEntityInternalId())
                .externalArrangementId(request.getExternalArrangementId())
                .dateRangeStart(request.getDateRangeStart())
                .dateRangeEnd(request.getDateRangeEnd())
                .additions(request.getAdditions())
                .build();
    }

    /**
     * Builds ingestion response for API endpoint.
     *
     * @param response PaymentOrderIngestResponse
     * @return IngestionResponse
     */
    private ResponseEntity<PaymentOrderIngestionResponse> mapIngestionToResponse(PaymentOrderIngestResponse response) {
        System.out.println("mapping ingestion to response!!!!!!");
        return new ResponseEntity<>(
                new PaymentOrderIngestionResponse()
                        .withPayment(
                                response.getPaymentOrderPostResponses()
                                        .stream()
                                        .map(paymentOrderMapper::mapStreamToComposition)
                                        .collect(Collectors.toList())),
                HttpStatus.CREATED);
    }
}
