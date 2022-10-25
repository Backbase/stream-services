package com.backbase.stream.portfolio.controller.it;

import static com.backbase.stream.portfolio.util.PortfolioHttpTestUtil.X_TID_HEADER_NAME;
import static com.backbase.stream.portfolio.util.PortfolioHttpTestUtil.X_TID_HEADER_VALUE;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import com.backbase.stream.portfolio.PortfolioHttpApplication;
import com.backbase.stream.portfolio.model.TransactionCategory;
import com.backbase.stream.portfolio.util.PortfolioHttpTestUtil;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

/**
 * WealthPortfolioAllocations IT.
 * 
 * @author Vladimir Kirchev
 *
 */
@SpringBootTest(classes = PortfolioHttpApplication.class)
@ContextConfiguration(classes = {WealthTransactionCategoriesIT.TestConfiguration.class})
@AutoConfigureWebTestClient(timeout = "20000")
@ActiveProfiles({"it"})
class WealthTransactionCategoriesIT {

    @RegisterExtension
    static WireMockExtension wireMockServer =
            WireMockExtension.newInstance().options(WireMockConfiguration.wireMockConfig().dynamicPort()).build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.client.provider.bb.token-uri",
                () -> String.format("%s/oauth/token", wireMockServer.baseUrl()));

        registry.add("backbase.stream.dbs.portfolio-base-url",
                () -> String.format("%s/portfolio", wireMockServer.baseUrl()));
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldIngestTransactionCategories() throws Exception {
        // Given
        setupWireMock();

        List<TransactionCategory> transactionCategories = PortfolioHttpTestUtil.getTransactionCategories();

        // When
        webTestClient.post().uri("/portfolios/transaction-categories/batch").header("Content-Type", "application/json")
                .header(X_TID_HEADER_NAME, X_TID_HEADER_VALUE).bodyValue(transactionCategories).exchange()
                .expectStatus().isEqualTo(200);

        // Then
        wireMockServer.verify(WireMock
                .getRequestedFor(WireMock.urlEqualTo("/portfolio/integration-api/v1/transaction-categories"))
                .withHeader(X_TID_HEADER_NAME, WireMock.equalTo(X_TID_HEADER_VALUE)));

        wireMockServer.verify(WireMock
                .postRequestedFor(WireMock.urlEqualTo("/portfolio/integration-api/v1/transaction-categories"))
                .withHeader(X_TID_HEADER_NAME, WireMock.equalTo(X_TID_HEADER_VALUE)));

        Assertions.assertTrue(wireMockServer.findAllUnmatchedRequests().isEmpty());
    }

    private void setupWireMock() {
        wireMockServer.stubFor(WireMock.post("/oauth/token").willReturn(WireMock.aResponse()
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody("{\"access_token\": \"access-token\",\"expires_in\": 600,\"refresh_expires_in\": 1800,"
                        + "\"refresh_token\": \"refresh-token\",\"token_type\": \"bearer\",\"id_token\": \"id-token\","
                        + "\"not-before-policy\": 1633622545,"
                        + "\"session_state\": \"72a28739-3d20-4965-bd86-64410df53d04\",\"scope\": \"openid\"}")));

        wireMockServer.stubFor(WireMock.get("/portfolio/integration-api/v1/transaction-categories")
                .willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[{\"key\":\"key0\", \"alias\": \"alias0\", \"description\": \"description0\"}]")));

        wireMockServer.stubFor(WireMock.post("/portfolio/integration-api/v1/transaction-categories").willReturn(
                WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));
    }

    public static class TestConfiguration {
        @Bean
        public WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }
}
