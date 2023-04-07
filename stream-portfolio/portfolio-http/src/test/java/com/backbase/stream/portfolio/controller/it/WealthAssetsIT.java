package com.backbase.stream.portfolio.controller.it;

import static com.backbase.stream.portfolio.util.PortfolioHttpTestUtil.X_TID_HEADER_NAME;
import static com.backbase.stream.portfolio.util.PortfolioHttpTestUtil.X_TID_HEADER_VALUE;

import com.backbase.stream.portfolio.model.AssetClassBundle;
import com.backbase.stream.portfolio.util.PortfolioHttpTestUtil;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

/**
 * WealthAssets IT.
 *
 * @author Vladimir Kirchev
 */
@SpringBootTest
@WireMockTest(httpPort = 10000)
@AutoConfigureWebTestClient(timeout = "20000")
@ActiveProfiles({"it"})
class WealthAssetsIT {
    @Autowired private WebTestClient webTestClient;

    @Test
    void shouldIngestRegionBundles() throws Exception {
        // Given
        setupWireMock();

        List<AssetClassBundle> assetClassBundles = PortfolioHttpTestUtil.getAssetClasseBundles();

        // When
        webTestClient
                .post()
                .uri("/portfolios/asset-classes/batch")
                .header("Content-Type", "application/json")
                .header(X_TID_HEADER_NAME, X_TID_HEADER_VALUE)
                .bodyValue(assetClassBundles)
                .exchange()
                .expectStatus()
                .isEqualTo(200);

        // Then
        WireMock.verify(
                WireMock.getRequestedFor(
                                WireMock.urlEqualTo(
                                        "/portfolio/integration-api/v1/asset-classes?from=0&size=2147483647"))
                        .withHeader(X_TID_HEADER_NAME, WireMock.equalTo(X_TID_HEADER_VALUE)));

        Assertions.assertTrue(WireMock.findUnmatchedRequests().isEmpty());
    }

    private void setupWireMock() {
        WireMock.stubFor(
                WireMock.post("/oauth/token")
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
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
                WireMock.get("/portfolio/integration-api/v1/asset-classes?from=0&size=2147483647")
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody("{\"assetClasses\":[]}")));

        WireMock.stubFor(
                WireMock.get(
                                "/portfolio/integration-api/v1/asset-classes/OvBckZySky/sub-asset-classes?from=0&size=2147483647")
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody("{\"subAssetClasses\":[]}")));

        WireMock.stubFor(
                WireMock.get(
                                "/portfolio/integration-api/v1/asset-classes/iWDEjAewOq/sub-asset-classes?from=0&size=2147483647")
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody("{\"subAssetClasses\":[]}")));

        WireMock.stubFor(
                WireMock.get(
                                "/portfolio/integration-api/v1/asset-classes/KAmfp8dZWo/sub-asset-classes?from=0&size=2147483647")
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody("{\"subAssetClasses\":[]}")));

        WireMock.stubFor(
                WireMock.get(
                                "/portfolio/integration-api/v1/asset-classes/L1mC78IIVj/sub-asset-classes?from=0&size=2147483647")
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody("{\"subAssetClasses\":[]}")));

        WireMock.stubFor(
                WireMock.post("/portfolio/integration-api/v1/asset-classes")
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody("")));

        WireMock.stubFor(
                WireMock.post(
                                "/portfolio/integration-api/v1/asset-classes/OvBckZySky/sub-asset-classes")
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody("")));

        WireMock.stubFor(
                WireMock.post(
                                "/portfolio/integration-api/v1/asset-classes/iWDEjAewOq/sub-asset-classes")
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody("")));

        WireMock.stubFor(
                WireMock.post(
                                "/portfolio/integration-api/v1/asset-classes/KAmfp8dZWo/sub-asset-classes")
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody("")));

        WireMock.stubFor(
                WireMock.post(
                                "/portfolio/integration-api/v1/asset-classes/L1mC78IIVj/sub-asset-classes")
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody("")));
    }
}
