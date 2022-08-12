package com.backbase.stream.compositions.paymentorders.core.service.impl;

import org.springframework.stereotype.Service;
import com.backbase.stream.compositions.paymentorder.integration.client.PaymentOrderIntegrationApi;
import com.backbase.stream.compositions.paymentorder.integration.client.model.PaymentOrderPostRequest;
import com.backbase.stream.compositions.paymentorder.integration.client.model.PullPaymentOrderResponse;
import com.backbase.stream.compositions.paymentorders.core.mapper.PaymentOrderMapper;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestPullRequest;
import com.backbase.stream.compositions.paymentorders.core.service.PaymentOrderIntegrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentOrderIntegrationServiceImpl implements PaymentOrderIntegrationService {

    private final PaymentOrderIntegrationApi paymentOrderIntegrationApi;
    private final PaymentOrderMapper paymentOrderMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public Flux<PaymentOrderPostRequest> pullPaymentOrder(PaymentOrderIngestPullRequest ingestPullRequest) {

//        return paymentOrderIntegrationApi
//                .pullPaymentOrders(
//                        paymentOrderMapper.mapStreamToIntegration(ingestPullRequest))
//                .flatMapIterable(PullPaymentOrderResponse::getPaymentOrder);
        return createFakeIntegrationResponse()
                .flatMapIterable(PullPaymentOrderResponse::getPaymentOrder);
    }

    private Mono<PullPaymentOrderResponse> createFakeIntegrationResponse() {
        try {
            PullPaymentOrderResponse pullPaymentOrderResponse = new PullPaymentOrderResponse();

            ObjectMapper mapper = JsonMapper.builder()
                    .addModule(new JavaTimeModule())
                    .build();

            PaymentOrderPostRequest paymentOrderPostRequest = mapper
                    .readValue(jsonPostRequest(), PaymentOrderPostRequest.class);

            System.out.println("POJO pulled: " + paymentOrderPostRequest);


//        PaymentOrderPostRequest paymentOrderPostRequest = new PaymentOrderPostRequest();
//        paymentOrderPostRequest.setBankReferenceId("bankRef");
//        paymentOrderPostRequest.setTotalAmount(new Currency()
//                .withAmount("2.00")
//                .withCurrencyCode("USD"));
            pullPaymentOrderResponse.addPaymentOrderItem(paymentOrderPostRequest);

            return Mono.just(pullPaymentOrderResponse);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private String jsonPostRequest(){
        return "{\n" +
                "  \"id\": \"729190df-a421-4937-94fd-5e1a3da132cc\",\n" +
                "  \"internalUserId\": \"d5ba7e3b-b1e4-42ee-989f-860e97386dc1\",\n" +
                "  \"originator\": {\n" +
                "    \"name\": \"John Doe\"\n" +
                "  },\n" +
                "  \"originatorAccount\": {\n" +
                "    \"identification\": {\n" +
                "      \"identification\": \"56bf7f66-4417-4cc6-8aa2-a467227d3067\",\n" +
                "      \"schemeName\": \"ID\"\n" +
                "    },\n" +
                "    \"arrangementId\": \"56bf7f66-4417-4cc6-8aa2-a467227d3067\",\n" +
                "    \"externalArrangementId\": \"0000099188-S10\"\n" +
                "  },\n" +
                "  \"totalAmount\": {\n" +
                "    \"amount\": \"10.29\",\n" +
                "    \"currencyCode\": \"USD\"\n" +
                "  },\n" +
                "  \"batchBooking\": false,\n" +
                "  \"instructionPriority\": \"HIGH\",\n" +
                "  \"status\": \"ENTERED\",\n" +
                "  \"requestedExecutionDate\": \"2021-01-20\",\n" +
                "  \"paymentMode\": \"RECURRING\",\n" +
                "  \"paymentType\": \"SEPA_CREDIT_TRANSFER\",\n" +
                "  \"schedule\": {\n" +
                "    \"transferFrequency\": \"MONTHLY\",\n" +
                "    \"on\": 1,\n" +
                "    \"startDate\": \"2021-02-01\",\n" +
                "    \"endDate\": \"2021-05-01\",\n" +
                "    \"nextExecutionDate\": \"2021-03-01\",\n" +
                "    \"every\": \"1\"\n" +
                "  },\n" +
                "  \"transferTransactionInformation\": {\n" +
                "    \"instructedAmount\": {\n" +
                "      \"amount\": \"10.29\",\n" +
                "      \"currencyCode\": \"EUR\"\n" +
                "    },\n" +
                "    \"counterparty\": {\n" +
                "      \"name\": \"Dagobert Danford\"\n" +
                "    },\n" +
                "    \"counterpartyAccount\": {\n" +
                "      \"identification\": {\n" +
                "        \"identification\": \"41638cee-b494-41f0-a1e3-27405e90da7d\",\n" +
                "        \"schemeName\": \"ID\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"remittanceInformation\": {\n" +
                "      \"type\": \"UNSTRUCTURED\",\n" +
                "      \"content\": \"Return a debt\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"audit\": {\n" +
                "    \"timestamp\": \"2021-01-20T14:33:13Z\",\n" +
                "    \"user\": \"bbuser\"\n" +
                "  },\n" +
                "  \"intraLegalEntity\": false,\n" +
                "  \"serviceAgreementId\": \"4028809b8190b88a018190c9269b0009\",\n" +
                "  \"originatorAccountCurrency\": \"USD\"\n" +
                "}";
    }
}
