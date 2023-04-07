package com.backbase.stream.portfolio.controller.it;

import static com.backbase.stream.portfolio.util.PortfolioHttpTestUtil.X_TID_HEADER_NAME;
import static com.backbase.stream.portfolio.util.PortfolioHttpTestUtil.X_TID_HEADER_VALUE;

import com.backbase.stream.portfolio.model.TransactionCategory;
import com.backbase.stream.portfolio.util.PortfolioHttpTestUtil;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * WealthTransactionCategories IT.
 *
 * @author Vladimir Kirchev
 */
@SpringBootTest
@WireMockTest(httpPort = 10000)
@AutoConfigureWebTestClient(timeout = "20000")
@ActiveProfiles({"it"})
class WealthTransactionCategoriesIT {
  @Autowired private WebTestClient webTestClient;

  @Test
  void shouldIngestTransactionCategories() throws Exception {
    // Given
    setupWireMock();

    List<TransactionCategory> transactionCategories =
        PortfolioHttpTestUtil.getTransactionCategories();

    // When
    webTestClient
        .post()
        .uri("/portfolios/transaction-categories/batch")
        .header("Content-Type", "application/json")
        .header(X_TID_HEADER_NAME, X_TID_HEADER_VALUE)
        .bodyValue(transactionCategories)
        .exchange()
        .expectStatus()
        .isEqualTo(200);

    // Then
    WireMock.verify(
        WireMock.getRequestedFor(
                WireMock.urlEqualTo("/portfolio/integration-api/v1/transaction-categories"))
            .withHeader(X_TID_HEADER_NAME, WireMock.equalTo(X_TID_HEADER_VALUE)));

    WireMock.verify(
        WireMock.postRequestedFor(
                WireMock.urlEqualTo("/portfolio/integration-api/v1/transaction-categories"))
            .withHeader(X_TID_HEADER_NAME, WireMock.equalTo(X_TID_HEADER_VALUE)));

    Assertions.assertTrue(WireMock.findUnmatchedRequests().isEmpty());
  }

  private void setupWireMock() {
    WireMock.stubFor(
        WireMock.post("/oauth/token")
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody(
                        "{\"access_token\":"
                            + " \"access-token\",\"expires_in\":"
                            + " 600,\"refresh_expires_in\":"
                            + " 1800,\"refresh_token\":"
                            + " \"refresh-token\",\"token_type\":"
                            + " \"bearer\",\"id_token\":"
                            + " \"id-token\",\"not-before-policy\":"
                            + " 1633622545,\"session_state\":"
                            + " \"72a28739-3d20-4965-bd86-64410df53d04\",\"scope\":"
                            + " \"openid\"}")));

    WireMock.stubFor(
        WireMock.get("/portfolio/integration-api/v1/transaction-categories")
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody(
                        "[{\"key\":\"key0\", \"alias\": \"alias0\","
                            + " \"description\": \"description0\"}]")));

    WireMock.stubFor(
        WireMock.post("/portfolio/integration-api/v1/transaction-categories")
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody("")));
  }
}
