package com.backbase.stream.transaction;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.backbase.dbs.transaction.api.service.ApiClient;
import com.backbase.dbs.transaction.api.service.v4.TransactionPresentationServiceApi;
import com.backbase.dbs.transaction.api.service.v4.model.CreditDebitIndicator;
import com.backbase.dbs.transaction.api.service.v4.model.Currency;
import com.backbase.dbs.transaction.api.service.v4.model.TransactionsPostRequestBody;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.test.StepVerifier;

/**
 * HTTP-level contract: TransactionTaskExecutor posts service-api v4 with required sequenceNumber.
 */
class TransactionTaskExecutorWireMockIT {

    private static final String TM_POST_PATH = "/transaction-manager/service-api/v4/transactions";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private TransactionTaskExecutor executor;

    @BeforeEach
    void setUp() {
        ApiClient apiClient = new ApiClient()
            .setBasePath("http://localhost:" + wireMock.getPort() + "/transaction-manager");
        executor = new TransactionTaskExecutor(new TransactionPresentationServiceApi(apiClient));
        wireMock.resetAll();
        stubSuccessfulPost();
    }

    @Test
    void executeTask_defaultsMissingSequenceNumberToZero_andPostsV4() {
        TransactionsPostRequestBody body = minimalBody("ext-non-numeric")
            .sequenceNumber(null);

        StepVerifier.create(executor.executeTask(new TransactionTask("uow-1", List.of(body))))
            .assertNext(task -> {
                assertNotNull(task.getResponse());
                assertEquals(1, task.getResponse().size());
                assertEquals("ext-non-numeric", task.getResponse().get(0).getExternalId());
            })
            .verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo(TM_POST_PATH))
            .withRequestBody(matchingJsonPath("$[0].sequenceNumber", equalTo("0")))
            .withRequestBody(matchingJsonPath("$[0].externalId", equalTo("ext-non-numeric"))));
    }

    @Test
    void executeTask_fallsBackToNumericExternalId_whenSequenceNumberInvalid() {
        TransactionsPostRequestBody body = minimalBody("42")
            .sequenceNumber("abc");

        StepVerifier.create(executor.executeTask(new TransactionTask("uow-2", List.of(body))))
            .assertNext(task -> assertEquals(1, task.getResponse().size()))
            .verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo(TM_POST_PATH))
            .withRequestBody(matchingJsonPath("$[0].sequenceNumber", equalTo("42"))));
    }

    @Test
    void executeTask_keepsValidSequenceNumber() {
        TransactionsPostRequestBody body = minimalBody("ext-1")
            .sequenceNumber("99");

        StepVerifier.create(executor.executeTask(new TransactionTask("uow-3", List.of(body))))
            .assertNext(task -> assertEquals(1, task.getResponse().size()))
            .verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo(TM_POST_PATH))
            .withRequestBody(matchingJsonPath("$[0].sequenceNumber", equalTo("99"))));
    }

    private void stubSuccessfulPost() {
        wireMock.stubFor(post(urlEqualTo(TM_POST_PATH))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("""
                    [{"id":"tm-1","externalId":"ext-non-numeric"}]
                    """)));
    }

    private static TransactionsPostRequestBody minimalBody(String externalId) {
        return new TransactionsPostRequestBody()
            .externalId(externalId)
            .externalArrangementId("arr-1")
            .description("wiremock ingest")
            .type("Payment")
            .bookingDate(LocalDate.of(2026, 11, 13))
            .creditDebitIndicator(CreditDebitIndicator.DBIT)
            .transactionAmountCurrency(new Currency()
                .amount(new BigDecimal("10.00"))
                .currencyCode("EUR"));
    }
}
