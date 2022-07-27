package com.backbase.stream.webclient;

import static org.assertj.core.api.Assertions.assertThat;

import com.backbase.buildingblocks.webclient.InterServiceWebClientConfiguration;
import com.backbase.buildingblocks.webclient.WebClientConstants;
import com.backbase.stream.webclient.filter.HeadersForwardingClientFilter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.reactive.function.client.WebClient;


@Slf4j
@SpringJUnitConfig
public class DbsWebClientConfigurationTest {

    ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    public void webClientCustomizerTest() {
        contextRunner
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withUserConfiguration(DbsWebClientConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(WebClient.class);
                assertThat(context).getBean(WebClientConstants.INTER_SERVICE_WEB_CLIENT_NAME)
                    .extracting("builder.filters")
                    .asList()
                    .anyMatch(HeadersForwardingClientFilter.class::isInstance);
            });
    }

    @Test
    public void shouldCreateFallbackClientRegistrationWithOverriddenTokenUriTest() {
        contextRunner
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withUserConfiguration(DbsWebClientConfiguration.class)
            .run(context -> {
                var tokenUri = context.getBean(ReactiveClientRegistrationRepository.class)
                    .findByRegistrationId(WebClientConstants.DEFAULT_REGISTRATION_ID)
                    .block()
                    .getProviderDetails()
                    .getTokenUri();
                Assertions.assertEquals("http://token-converter:8080/oauth/token", tokenUri);
            });
    }

    @Test
    public void shouldCreateFallbackClientRegistrationWithCustomEnvVarTest() {
        contextRunner
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withUserConfiguration(DbsWebClientConfiguration.class)
            .withSystemProperties("DBS_TOKEN_URI=http://my-custom-endpoint/oauth/token")
            .run(context -> {
                var tokenUri = context.getBean(ReactiveClientRegistrationRepository.class)
                    .findByRegistrationId(WebClientConstants.DEFAULT_REGISTRATION_ID)
                    .block()
                    .getProviderDetails()
                    .getTokenUri();
                Assertions.assertEquals("http://my-custom-endpoint/oauth/token", tokenUri);
            });
    }

    @Test
    public void shouldNotCreateFallbackClientRegistrationTest() {
        contextRunner
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withUserConfiguration(DbsWebClientConfiguration.class)
            .withPropertyValues(
                "spring.security.oauth2.client.registration.bb.authorization-grant-type=client_credentials",
                "spring.security.oauth2.client.registration.bb.client-id=bb-client",
                "spring.security.oauth2.client.registration.bb.client-secret=bb-secret",
                "spring.security.oauth2.client.registration.bb.client-authentication-method=post",
                "spring.security.oauth2.client.provider.bb.token-uri=http://my-custom-endpoint/oauth/token")
            .run(context -> {
                var tokenUri = context.getBean(ReactiveClientRegistrationRepository.class)
                    .findByRegistrationId(WebClientConstants.DEFAULT_REGISTRATION_ID)
                    .block()
                    .getProviderDetails()
                    .getTokenUri();
                Assertions.assertEquals("http://my-custom-endpoint/oauth/token", tokenUri);
            });
    }

    @Test
    public void shouldUseCustomRegistrationTest() {
        contextRunner
            .withBean(WebClientAutoConfiguration.class)
            .withBean(InterServiceWebClientConfiguration.class)
            .withUserConfiguration(DbsWebClientConfiguration.class)
            .withPropertyValues(
                "spring.security.oauth2.client.registration.dbs.authorization-grant-type=client_credentials",
                "spring.security.oauth2.client.registration.dbs.client-id=bb-client",
                "spring.security.oauth2.client.registration.dbs.client-secret=bb-secret",
                "spring.security.oauth2.client.registration.dbs.client-authentication-method=post",
                "spring.security.oauth2.client.provider.dbs.token-uri=http://my-custom-endpoint/oauth/token")
            .run(context -> {
                var repository = context.getBean(ReactiveClientRegistrationRepository.class);
                var defaultRegistration = repository.findByRegistrationId(WebClientConstants.DEFAULT_REGISTRATION_ID)
                    .blockOptional();
                Assertions.assertTrue(defaultRegistration.isEmpty(),
                    "It should not create the default registration id.");

                var tokenUri = repository.findByRegistrationId("dbs")
                    .block()
                    .getProviderDetails()
                    .getTokenUri();
                Assertions.assertEquals("http://my-custom-endpoint/oauth/token", tokenUri);
            });
    }

}
