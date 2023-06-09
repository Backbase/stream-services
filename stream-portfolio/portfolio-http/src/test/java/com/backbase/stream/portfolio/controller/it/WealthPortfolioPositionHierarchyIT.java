package com.backbase.stream.portfolio.controller.it;

import static com.backbase.stream.portfolio.util.PortfolioHttpTestUtil.X_TID_HEADER_NAME;
import static com.backbase.stream.portfolio.util.PortfolioHttpTestUtil.X_TID_HEADER_VALUE;

import com.backbase.stream.portfolio.model.HierarchyBundle;
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
 * WealthPortfolioPositionHierarchy IT.
 *
 * @author Vladimir Kirchev
 */
@SpringBootTest
@WireMockTest(httpPort = 10000)
@AutoConfigureWebTestClient(timeout = "20000")
@ActiveProfiles({"it"})
class WealthPortfolioPositionHierarchyIT {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldIngestTransactionCategories() throws Exception {
        // Given
        setupWireMock();

        List<HierarchyBundle> hierarchyBundles = PortfolioHttpTestUtil.getHierarchyBundles();

        // When
        webTestClient
            .post()
            .uri("/portfolios/hierarchies/batch")
            .header("Content-Type", "application/json")
            .header(X_TID_HEADER_NAME, X_TID_HEADER_VALUE)
            .bodyValue(hierarchyBundles)
            .exchange()
            .expectStatus()
            .isEqualTo(200);

        // Then
        WireMock.verify(
            WireMock.putRequestedFor(
                    WireMock.urlEqualTo("/portfolio/integration-api/v1/portfolios/1234/hierarchies"))
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
            WireMock.put("/portfolio/integration-api/v1/portfolios/1234/hierarchies")
                .willReturn(
                    WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));
    }
}
