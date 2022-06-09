package com.backbase.stream.webclient;

import com.backbase.buildingblocks.webclient.InterServiceWebClientCustomizer;
import com.backbase.stream.webclient.configuration.DbsWebClientConfigurationProperties;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


@Slf4j
@Disabled
public class DbsWebClientConfigurationTest {

    private static final int PORT = 12378;

    @Test
    public void testWebClient() {

        String tokenUri = "http://localhost:" + PORT + "/api/token-converter/oauth/token";

        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("bb")
            .clientAuthenticationMethod(ClientAuthenticationMethod.POST)
            .clientId("bb-client")
            .clientSecret("bb-secret")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri(tokenUri)
            .build();

        DbsWebClientConfiguration dbsWebClientConfiguration = new DbsWebClientConfiguration();

        List<ClientRegistration> registrations = Collections.singletonList(clientRegistration);
        InMemoryReactiveClientRegistrationRepository registrationRepository = new InMemoryReactiveClientRegistrationRepository(
            registrations);

        InMemoryReactiveOAuth2AuthorizedClientService clientService = new
            InMemoryReactiveOAuth2AuthorizedClientService(registrationRepository);

        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
            registrationRepository,
            clientService);
        ClientCredentialsReactiveOAuth2AuthorizedClientProvider clientProvider =
            new ClientCredentialsReactiveOAuth2AuthorizedClientProvider();

        oAuth2AuthorizedClientManager.setAuthorizedClientProvider(clientProvider);

        WebClient.Builder builder = WebClient.builder();

        InterServiceWebClientCustomizer webClientCustomizer = dbsWebClientConfiguration.webClientCustomizer(
            new DbsWebClientConfigurationProperties());

        webClientCustomizer.customize(builder);

        WebClient webClient = builder.build();

        Assertions.assertNotNull(webClient);

        String testUrl = "http://localhost:" + PORT + "/hello-world";
        Mono<ClientResponse> exchange = webClient.get().uri(testUrl)
            .exchange()
            .doOnNext(clientResponse -> {
                log.info("Client Response Status: " + clientResponse.rawStatusCode());
            })
            .onErrorResume(HttpClientErrorException.class, error -> {
                log.error("Client Error Response: " + error.getResponseBodyAsString());
                return Mono.error(error);
            });

        StepVerifier.create(exchange)
            .expectNextCount(1)
            .verifyComplete();

    }

}
