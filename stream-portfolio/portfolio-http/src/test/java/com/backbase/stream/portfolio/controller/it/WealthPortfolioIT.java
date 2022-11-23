package com.backbase.stream.portfolio.controller.it;

import static com.backbase.stream.portfolio.controller.it.WealthPortfolioIT.NotEmptyPattern.notEmpty;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import com.backbase.stream.portfolio.model.Portfolio;
import com.backbase.stream.portfolio.util.PortfolioHttpTestUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;

/**
 * WealthPortfolio IT.
 * 
 * @author Vladimir Kirchev
 *
 */
@SpringBootTest
@WireMockTest(httpPort = 10000)
@AutoConfigureWebTestClient(timeout = "20000")
@ActiveProfiles({"it"})
class WealthPortfolioIT {
    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldIngestRegionBundles() throws Exception {
        // Given
        setupWireMock();

        List<Portfolio> portfolios = PortfolioHttpTestUtil.getPortfolios();

        // When
        webTestClient.post()
                .uri("/portfolios/batch")
                .header("Content-Type", "application/json")
                .header("X-TID", "tenant-id")
                .bodyValue(portfolios)
                .exchange()
                .expectStatus()
                .isEqualTo(200);

        // Then
        verify(WireMock
                .getRequestedFor(WireMock.urlEqualTo("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA"))
                .withHeader("X-TID", WireMock.equalTo("tenant-id"))
                .withHeader("X-B3-TraceId", notEmpty())
                .withHeader("X-B3-SpanId", notEmpty()));

        Assertions.assertTrue(WireMock.findUnmatchedRequests().isEmpty());
    }

    private void setupWireMock() {
        stubFor(WireMock.post("/oauth/token")
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                                "{\"access_token\": \"access-token\",\"expires_in\": 600,\"refresh_expires_in\": 1800,"
                                        + "\"refresh_token\": \"refresh-token\",\"token_type\": \"bearer\",\"id_token\": \"id-token\","
                                        + "\"not-before-policy\": 1633622545,"
                                        + "\"session_state\": \"72a28739-3d20-4965-bd86-64410df53d04\",\"scope\": \"openid\"}")));

        stubFor(WireMock.get("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA")
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                                "{\"code\": \"testCode\", \"arrangementId\": \"testArrangementId\", \"name\": \"testName\"}")));

        stubFor(WireMock.post("/portfolio/integration-api/v1/portfolios")
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"regions\":[]}")));

        stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA")
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"regions\":[]}")));
    }

    static class NotEmptyPattern extends StringValuePattern {
        public NotEmptyPattern(@JsonProperty("something") String expectedValue) {
            super(expectedValue);
        }

        @Override
        public MatchResult match(String value) {
            return MatchResult.of(StringUtils.isNotBlank(value));
        }

        public static NotEmptyPattern notEmpty() {
            return new NotEmptyPattern("(always)");
        }
    }
}
