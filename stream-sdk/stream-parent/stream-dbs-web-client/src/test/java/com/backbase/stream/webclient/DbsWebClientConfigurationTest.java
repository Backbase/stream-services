package com.backbase.stream.webclient;

import com.backbase.stream.webclient.configuration.DbsWebClientConfigurationProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
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

import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;


@Slf4j
@Disabled
public class DbsWebClientConfigurationTest {

    private static final int PORT = 12378;

    @Test
    public void testDateFormat() {
        DbsWebClientConfiguration dbsWebClientConfiguration = new DbsWebClientConfiguration();
        DateFormat dateFormat = dbsWebClientConfiguration.dateFormat();
        Assertions.assertEquals( dateFormat.getClass(), StdDateFormat.class);
    }

    @Test
    public void testObjectMapper() throws JsonProcessingException {
        DbsWebClientConfiguration dbsWebClientConfiguration = new DbsWebClientConfiguration();
        DateFormat dateFormat = dbsWebClientConfiguration.dateFormat();
        ObjectMapper objectMapper = dbsWebClientConfiguration.objectMapper(dateFormat);
        String o = "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule";
        Assertions.assertTrue(objectMapper.getRegisteredModuleIds().contains(o));

        LocalDateTime localDateTime = LocalDateTime.of(1980, 6, 11, 12, 32);
        String expected = "\"1980-06-11T12:32:00\"";
        String actual = objectMapper.writeValueAsString(localDateTime);
        Assertions.assertEquals("Date Format is Wrong", expected, actual);

        ZoneOffset zoneOffset = ZoneOffset.of("-02:00");
        OffsetDateTime offsetDateTime = OffsetDateTime.of(localDateTime, zoneOffset);
        expected = "\"1980-06-11T12:32:00-02:00\"";
        actual = objectMapper.writeValueAsString(offsetDateTime);

        System.out.println(offsetDateTime);
        Assertions.assertEquals( expected, actual);
    }

    @Test
    public void testWebClient() {

        String tokenUri = "http://localhost:" + PORT + "/api/token-converter/oauth/token";

        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("dbs")
            .clientAuthenticationMethod(ClientAuthenticationMethod.POST)
            .clientId("bb-client")
            .clientSecret("bb-secret")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri(tokenUri)
            .build();

        DbsWebClientConfiguration dbsWebClientConfiguration = new DbsWebClientConfiguration();
        DateFormat dateFormat = dbsWebClientConfiguration.dateFormat();
        ObjectMapper objectMapper = dbsWebClientConfiguration.objectMapper(dateFormat);

        List<ClientRegistration> registrations = Collections.singletonList(clientRegistration);
        InMemoryReactiveClientRegistrationRepository registrationRepository = new InMemoryReactiveClientRegistrationRepository(registrations);

        InMemoryReactiveOAuth2AuthorizedClientService clientService = new
            InMemoryReactiveOAuth2AuthorizedClientService(registrationRepository);

        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
            registrationRepository,
            clientService);
        ClientCredentialsReactiveOAuth2AuthorizedClientProvider clientProvider =
            new ClientCredentialsReactiveOAuth2AuthorizedClientProvider();


        oAuth2AuthorizedClientManager.setAuthorizedClientProvider(clientProvider);

        WebClient.Builder builder = WebClient.builder();

        WebClient webClient = dbsWebClientConfiguration.dbsWebClient(
            objectMapper,
            oAuth2AuthorizedClientManager,
            builder,
            new DbsWebClientConfigurationProperties());

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