package com.backbase.stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.web.reactive.function.client.WebClient;

@Ignore
public class AbstractServiceIntegrationTestsTest {

    private static final int PORT = 12375;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(PORT);

    @Test
    public void testSetupWebBuilder() throws IOException {
        // Should use dbs web client
        AbstractServiceIntegrationTests abstractServiceIntegrationTests = new AbstractServiceIntegrationTests();
        String tokenUri = "http://localhost:" + PORT + "/api/token-converter/oauth/token";
        WebClient webClient = abstractServiceIntegrationTests.setupWebClientBuilder(tokenUri,
            "bb-client",
            "bb-secret");
        String body = "Hi there!";
        stubFor(WireMock.get("/hello-world")
            .willReturn(ok(body)
                .withStatus(200)
                .withHeader("Content-Type", "plain/text")));
        stubFor(WireMock.post("/api/token-converter/oauth/token").willReturn(aResponse().withStatus(200)));

        Assert.assertNotNull(webClient);

        String result = webClient.get().uri("http://localhost:" + PORT + "/hello-world")
            .retrieve()
            .bodyToMono(String.class)
            .block();

        Assert.assertEquals(body, result);


    }

}
