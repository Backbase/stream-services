package com.backbase.stream.portfolio.saga.portfolio.it;

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
import com.backbase.stream.portfolio.model.WealthBundle;
import com.backbase.stream.portfolio.util.PortfolioHttpTestUtil;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

/**
 * PortfolioSaga IT.
 * 
 * @author Vladimir Kirchev
 *
 */
@SpringBootTest(classes = PortfolioHttpApplication.class)
@ContextConfiguration(classes = { PortfolioSagaIT.TestConfiguration.class })
@AutoConfigureWebTestClient(timeout = "20000")
@ActiveProfiles({ "it" })
class PortfolioSagaIT {

	@RegisterExtension
	static WireMockExtension wireMockServer = WireMockExtension.newInstance()
			.options(WireMockConfiguration.wireMockConfig().dynamicPort()).build();

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
	void shouldIngestPortfolios() throws Exception {
		// Given
		setupWireMock();

		List<WealthBundle> wealthBundles = PortfolioHttpTestUtil.getWealthBundles();

		// When
		webTestClient.post().uri("/portfolios").header("Content-Type", "application/json").header("X-TID", "tenant-id")
				.bodyValue(wealthBundles).exchange().expectStatus().isEqualTo(200);

		// Then
		wireMockServer.verify(WireMock
				.getRequestedFor(WireMock.urlEqualTo("/portfolio/integration-api/v1/regions?from=0&size=2147483647"))
				.withHeader("X-TID", WireMock.equalTo("tenant-id")));

		Assertions.assertTrue(wireMockServer.findAllUnmatchedRequests().isEmpty());
	}

	private void setupWireMock() {
		wireMockServer.stubFor(WireMock.post("/oauth/token").willReturn(WireMock.aResponse()
				.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.withBody("{\"access_token\": \"access-token\",\"expires_in\": 600,\"refresh_expires_in\": 1800,"
						+ "\"refresh_token\": \"refresh-token\",\"token_type\": \"bearer\",\"id_token\": \"id-token\","
						+ "\"not-before-policy\": 1633622545,"
						+ "\"session_state\": \"72a28739-3d20-4965-bd86-64410df53d04\",\"scope\": \"openid\"}")));

		wireMockServer.stubFor(WireMock.get("/portfolio/integration-api/v1/regions?from=0&size=2147483647")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"regions\":[]}")));

		wireMockServer.stubFor(WireMock.get("/portfolio/integration-api/v1/regions/EU/countries?from=0&size=2147483647")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"countries\":[]}")));

		wireMockServer.stubFor(WireMock.get("/portfolio/integration-api/v1/regions/US/countries?from=0&size=2147483647")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"countries\":[]}")));

		wireMockServer.stubFor(WireMock.post("/portfolio/integration-api/v1/regions").willReturn(WireMock.aResponse()
				.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("{\"regions\":[]}")));

		wireMockServer.stubFor(WireMock.post("/portfolio/integration-api/v1/countries").willReturn(WireMock.aResponse()
				.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("{\"countries\":[]}")));

		wireMockServer.stubFor(WireMock.get("/portfolio/integration-api/v1/asset-classes?from=0&size=2147483647")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"assetClasses\":[]}")));

		wireMockServer.stubFor(WireMock.post("/portfolio/integration-api/v1/portfolios").willReturn(
				WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock
				.get("/portfolio/integration-api/v1/portfolios/PS800002010/sub-portfolios/999629")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"code\":\"1234\", \"name\": \"testName\", \"valuation\": {\"amount\": 34.5, "
								+ "\"currencyCode\": \"EUR\"}, \"performance\": 54.6, \"percentOfParent\": 32.5}")));

		wireMockServer.stubFor(WireMock
				.put("/portfolio/integration-api/v1/portfolios/PS800002010/sub-portfolios/999629").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock
				.get("/portfolio/integration-api/v1/portfolios/PS800002008/sub-portfolios/997044")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"code\":\"1234\", \"name\": \"testName\", \"valuation\": {\"amount\": 34.5, "
								+ "\"currencyCode\": \"EUR\"}, \"performance\": 54.6, \"percentOfParent\": 32.5}")));

		wireMockServer.stubFor(WireMock
				.put("/portfolio/integration-api/v1/portfolios/PS800002008/sub-portfolios/997044").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock
				.put("/portfolio/integration-api/v1/portfolios/PS800002008/sub-portfolios/992412").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock
				.get("/portfolio/integration-api/v1/portfolios/PS800002008/sub-portfolios/992412")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"code\":\"1234\", \"name\": \"testName\", \"valuation\": {\"amount\": 34.5, "
								+ "\"currencyCode\": \"EUR\"}, \"performance\": 54.6, \"percentOfParent\": 32.5}")));

		wireMockServer.stubFor(WireMock
				.get("/portfolio/integration-api/v1/portfolios/PS800002008/sub-portfolios/992413")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"code\":\"1234\", \"name\": \"testName\", \"valuation\": {\"amount\": 34.5, "
								+ "\"currencyCode\": \"EUR\"}, \"performance\": 54.6, \"percentOfParent\": 32.5}")));

		wireMockServer.stubFor(WireMock
				.put("/portfolio/integration-api/v1/portfolios/PS800002008/sub-portfolios/992413").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock
				.get("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA/sub-portfolios/7514042661095")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"code\":\"1234\", \"name\": \"testName\", \"valuation\": {\"amount\": 34.5, "
								+ "\"currencyCode\": \"EUR\"}, \"performance\": 54.6, \"percentOfParent\": 32.5}")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA/sub-portfolios/7514042661095")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(WireMock
				.get("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA/sub-portfolios/4965926807606")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"code\":\"1234\", \"name\": \"testName\", \"valuation\": {\"amount\": 34.5, "
								+ "\"currencyCode\": \"EUR\"}, \"performance\": 54.6, \"percentOfParent\": 32.5}")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA/sub-portfolios/4965926807606")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(WireMock
				.get("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA/sub-portfolios/0681342020761")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"code\":\"1234\", \"name\": \"testName\", \"valuation\": {\"amount\": 34.5, "
								+ "\"currencyCode\": \"EUR\"}, \"performance\": 54.6, \"percentOfParent\": 32.5}")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA/sub-portfolios/0681342020761")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(WireMock
				.get("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA/sub-portfolios/5964103654347")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"code\":\"1234\", \"name\": \"testName\", \"valuation\": {\"amount\": 34.5, "
								+ "\"currencyCode\": \"EUR\"}, \"performance\": 54.6, \"percentOfParent\": 32.5}")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA/sub-portfolios/5964103654347")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(WireMock
				.get("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/sub-portfolios/7881342020761")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"code\":\"1234\", \"name\": \"testName\", \"valuation\": {\"amount\": 34.5, "
								+ "\"currencyCode\": \"EUR\"}, \"performance\": 54.6, \"percentOfParent\": 32.5}")));

		wireMockServer.stubFor(WireMock
				.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/sub-portfolios/7881342020761")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody(
						"{\"code\":\"1234\", \"name\": \"testName\", \"valuation\": {\"amount\": 34.5, " + "")));

		wireMockServer.stubFor(WireMock
				.get("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/sub-portfolios/3565926807606")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"code\":\"1234\", \"name\": \"testName\", \"valuation\": {\"amount\": 34.5, "
								+ "\"currencyCode\": \"EUR\"}, \"performance\": 54.6, \"percentOfParent\": 32.5}")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/sub-portfolios/3565926807606")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(WireMock
				.get("/portfolio/integration-api/v1/portfolios/PS800002008/sub-portfolios/997045")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"code\":\"1234\", \"name\": \"testName\", \"valuation\": {\"amount\": 34.5, "
								+ "\"currencyCode\": \"EUR\"}, \"performance\": 54.6, \"percentOfParent\": 32.5}")));

		wireMockServer.stubFor(WireMock
				.put("/portfolio/integration-api/v1/portfolios/PS800002008/sub-portfolios/997045").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock
				.get("/portfolio/integration-api/v1/portfolios/PS800002007/sub-portfolios/994570")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"code\":\"1234\", \"name\": \"testName\", \"valuation\": {\"amount\": 34.5, "
								+ "\"currencyCode\": \"EUR\"}, \"performance\": 54.6, \"percentOfParent\": 32.5}")));

		wireMockServer.stubFor(WireMock
				.put("/portfolio/integration-api/v1/portfolios/PS800002007/sub-portfolios/994570").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock
				.put("/portfolio/integration-api/v1/portfolios/PS800002007/sub-portfolios/994571")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"code\":\"1234\", \"name\": \"testName\", \"valuation\": {\"amount\": 34.5, "
								+ "\"currencyCode\": \"EUR\"}, \"performance\": 54.6, \"percentOfParent\": 32.5}")));

		wireMockServer.stubFor(WireMock
				.get("/portfolio/integration-api/v1/portfolios/PS800002007/sub-portfolios/994571")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"code\":\"1234\", \"name\": \"testName\", \"valuation\": {\"amount\": 34.5, "
								+ "\"currencyCode\": \"EUR\"}, \"performance\": 54.6, \"percentOfParent\": 32.5}")));

		wireMockServer.stubFor(
				WireMock.post("/portfolio/integration-api/v1/portfolios/PS800002007/sub-portfolios").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock.get("/portfolio/integration-api/v1/portfolios/960464/sub-portfolios/1427804")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"code\":\"1234\", \"name\": \"testName\", \"valuation\": {\"amount\": 34.5, "
								+ "\"currencyCode\": \"EUR\"}, \"performance\": 54.6, \"percentOfParent\": 32.5}")));

		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/960464/sub-portfolios/1427804")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(WireMock.get("/portfolio/integration-api/v1/portfolios/960464/sub-portfolios/1427805")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"code\":\"1234\", \"name\": \"testName\", \"valuation\": {\"amount\": 34.5, "
								+ "\"currencyCode\": \"EUR\"}, \"performance\": 54.6, \"percentOfParent\": 32.5}")));

		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/960464/sub-portfolios/1427805")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(WireMock
				.get("/portfolio/integration-api/v1/portfolios/PS800002010/sub-portfolios/999628")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"code\":\"1234\", \"name\": \"testName\", \"valuation\": {\"amount\": 34.5, "
								+ "\"currencyCode\": \"EUR\"}, \"performance\": 54.6, \"percentOfParent\": 32.5}")));

		wireMockServer.stubFor(WireMock
				.put("/portfolio/integration-api/v1/portfolios/PS800002010/sub-portfolios/999628").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock.post("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA/sub-portfolios")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(WireMock
				.post("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/sub-portfolios").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(
				WireMock.post("/portfolio/integration-api/v1/portfolios/PS800002008/sub-portfolios").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(
				WireMock.post("/portfolio/integration-api/v1/portfolios/960464/sub-portfolios").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(
				WireMock.get("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/sub-portfolios/5967103654349")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("{\"subPortfolios\": []}")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/sub-portfolios/5967103654349")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(
				WireMock.get("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/sub-portfolios/7514042661084")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("{\"subPortfolios\": []}")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/sub-portfolios/7514042661084")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(WireMock.get("/portfolio/integration-api/v1/portfolios/PS800002008")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody(
						"{\"code\": \"testCode\", \"arrangementId\": \"testArrangementId\", \"name\": \"testName\"}")));

		wireMockServer.stubFor(WireMock.get("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody(
						"{\"code\": \"testCode\", \"arrangementId\": \"testArrangementId\", \"name\": \"testName\"}")));

		wireMockServer.stubFor(WireMock.get("/portfolio/integration-api/v1/portfolios/PS800002010")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody(
						"{\"code\": \"testCode\", \"arrangementId\": \"testArrangementId\", \"name\": \"testName\"}")));

		wireMockServer.stubFor(WireMock.get("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody(
						"{\"code\": \"testCode\", \"arrangementId\": \"testArrangementId\", \"name\": \"testName\"}")));

		wireMockServer.stubFor(WireMock.get("/portfolio/integration-api/v1/portfolios/960464")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody(
						"{\"code\": \"testCode\", \"arrangementId\": \"testArrangementId\", \"name\": \"testName\"}")));

		wireMockServer.stubFor(WireMock.get("/portfolio/integration-api/v1/portfolios/PS800002007")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody(
						"{\"code\": \"testCode\", \"arrangementId\": \"testArrangementId\", \"name\": \"testName\"}")));

		wireMockServer.stubFor(WireMock.get("/portfolio/integration-api/v1/portfolios/960464")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody(
						"{\"code\": \"testCode\", \"arrangementId\": \"testArrangementId\", \"name\": \"testName\"}")));

		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/PS800002008").willReturn(
				WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA").willReturn(
				WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/PS800002010").willReturn(
				WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT").willReturn(
				WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/960464").willReturn(
				WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/PS800002007").willReturn(
				WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/960464").willReturn(
				WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/PS800002010/allocations").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/PS800002008/allocations").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/960464/allocations").willReturn(
				WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/PS800002007/allocations").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA/allocations")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/allocations")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/PS800002007/hierarchies").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/960464/hierarchies").willReturn(
				WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/PS800002008/hierarchies").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/PS800002010/hierarchies").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA/hierarchies")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/hierarchies")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/PS800002007/hierarchies").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/hierarchies")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA/cumulative-performances")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/cumulative-performances")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/960464/cumulative-performances")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(WireMock
				.put("/portfolio/integration-api/v1/portfolios/PS800002007/cumulative-performances").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock
				.put("/portfolio/integration-api/v1/portfolios/PS800002008/cumulative-performances").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock
				.put("/portfolio/integration-api/v1/portfolios/PS800002010/cumulative-performances").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock.get("/portfolio/integration-api/v1/benchmarks?from=0&size=2147483647")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"benchmarks\": []}")));

		wireMockServer.stubFor(WireMock.post("/portfolio/integration-api/v1/benchmarks").willReturn(
				WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(
				WireMock.delete("/portfolio/integration-api/v1/portfolios/PS800002007/valuations?granularity=DAILY")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(
				WireMock.delete("/portfolio/integration-api/v1/portfolios/PS800002007/valuations?granularity=WEEKLY")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(
				WireMock.delete("/portfolio/integration-api/v1/portfolios/PS800002007/valuations?granularity=MONTHLY")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(
				WireMock.delete("/portfolio/integration-api/v1/portfolios/PS800002007/valuations?granularity=QUARTERLY")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer
				.stubFor(WireMock.delete("/portfolio/integration-api/v1/portfolios/960464/valuations?granularity=DAILY")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(
				WireMock.delete("/portfolio/integration-api/v1/portfolios/960464/valuations?granularity=WEEKLY")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(
				WireMock.delete("/portfolio/integration-api/v1/portfolios/960464/valuations?granularity=MONTHLY")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(
				WireMock.delete("/portfolio/integration-api/v1/portfolios/960464/valuations?granularity=QUARTERLY")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(WireMock
				.delete("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA/valuations?granularity=DAILY")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(WireMock
				.delete("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA/valuations?granularity=WEEKLY")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(WireMock
				.delete("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA/valuations?granularity=MONTHLY")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(WireMock
				.delete("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA/valuations?granularity=QUARTERLY")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(WireMock
				.delete("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/valuations?granularity=DAILY")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(WireMock
				.delete("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/valuations?granularity=WEEKLY")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(WireMock
				.delete("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/valuations?granularity=MONTHLY")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(WireMock
				.delete("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/valuations?granularity=QUARTERLY")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(
				WireMock.delete("/portfolio/integration-api/v1/portfolios/PS800002008/valuations?granularity=DAILY")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(
				WireMock.delete("/portfolio/integration-api/v1/portfolios/PS800002008/valuations?granularity=WEEKLY")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(
				WireMock.delete("/portfolio/integration-api/v1/portfolios/PS800002008/valuations?granularity=MONTHLY")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(
				WireMock.delete("/portfolio/integration-api/v1/portfolios/PS800002008/valuations?granularity=QUARTERLY")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(
				WireMock.delete("/portfolio/integration-api/v1/portfolios/PS800002010/valuations?granularity=DAILY")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(
				WireMock.delete("/portfolio/integration-api/v1/portfolios/PS800002010/valuations?granularity=WEEKLY")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(
				WireMock.delete("/portfolio/integration-api/v1/portfolios/PS800002010/valuations?granularity=MONTHLY")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(
				WireMock.delete("/portfolio/integration-api/v1/portfolios/PS800002010/valuations?granularity=QUARTERLY")
						.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.withBody("")));

		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/960464/valuations").willReturn(
				WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/PS800002010/valuations").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/PS800002008/valuations").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA/valuations").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/PS800002007/valuations").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(
				WireMock.put("/portfolio/integration-api/v1/portfolios/PS800002010/valuations").willReturn(WireMock
						.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));

		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/valuations")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(WireMock.get("/portfolio/integration-api/v1/positions/072901772368896")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"externalId\": \"externalId\", \"instrumentId\":\"instrumentId\", "
								+ "\"portfolioCode\": \"portfolioCode\", \"subPortfolioCode\": \"subPortfolioCode\"}")));
		
		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/positions/072901772368896")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(WireMock.get("/portfolio/integration-api/v1/positions/072901772368866")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{\"externalId\": \"externalId\", \"instrumentId\":\"instrumentId\", "
								+ "\"portfolioCode\": \"portfolioCode\", \"subPortfolioCode\": \"subPortfolioCode\"}")));
		
		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/positions/072901772368866")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));

		wireMockServer.stubFor(WireMock.post("/portfolio/integration-api/v1/positions").willReturn(
				WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withBody("")));
		
		wireMockServer.stubFor(WireMock.get("/portfolio/integration-api/v1/transaction-categories")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("[{\"key\": \"key\", \"alias\": \"alias\", \"description\": \"description\"}]")));
		
		wireMockServer.stubFor(WireMock.put("/portfolio/integration-api/v1/transaction-categories")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));
		
		wireMockServer.stubFor(WireMock.post("/portfolio/integration-api/v1/transaction-categories")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));
		
		wireMockServer.stubFor(WireMock.delete("/portfolio/integration-api/v1/positions/072901772368866/transactions")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));
		
		wireMockServer.stubFor(WireMock.delete("/portfolio/integration-api/v1/positions/072901772368896/transactions")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));
		
		wireMockServer.stubFor(WireMock.delete("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/transactions")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));
		
		wireMockServer.stubFor(WireMock.post("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_ROHIT/transactions")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));
		
		wireMockServer.stubFor(WireMock.post("/portfolio/integration-api/v1/portfolios/ARRANGEMENT_SARA/transactions")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));
		
		wireMockServer.stubFor(WireMock.post("/portfolio/integration-api/v1/aggregate-portfolios")
				.willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("")));
	}

	public static class TestConfiguration {
		@Bean
		public WebClient.Builder webClientBuilder() {
			return WebClient.builder();
		}
	}
}
