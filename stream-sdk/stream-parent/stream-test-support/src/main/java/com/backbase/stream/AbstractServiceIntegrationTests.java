package com.backbase.stream;

import com.backbase.stream.webclient.DbsWebClientConfiguration;
import com.backbase.stream.webclient.configuration.DbsWebClientConfigurationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Hooks;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.Collections;
import java.util.List;


/**
 * Abstract Class to be used for Integration Tests.
 */
@Slf4j
public class AbstractServiceIntegrationTests {

    private DbsWebClientConfiguration dbsWebClientConfiguration = new DbsWebClientConfiguration();


    /**
     * Setup Web Client Builder to allow integration tests without Spring.
     */
    protected WebClient setupWebClientBuilder(String tokenUri, String clientId, String clientSecret) {
        Hooks.onOperatorDebug();

        DateFormat dateFormat = dbsWebClientConfiguration.dateFormat();
        ObjectMapper objectMapper = dbsWebClientConfiguration.objectMapper(dateFormat);

        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("dbs")
            .clientAuthenticationMethod(ClientAuthenticationMethod.POST)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri(tokenUri)
            .build();

        WebClient.Builder builder = WebClient.builder();


        List<ClientRegistration> registrations = Collections.singletonList(clientRegistration);
        InMemoryReactiveClientRegistrationRepository registrationRepository = new InMemoryReactiveClientRegistrationRepository(registrations);

        InMemoryReactiveOAuth2AuthorizedClientService clientService = new
            InMemoryReactiveOAuth2AuthorizedClientService(registrationRepository);

        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
            registrationRepository,
            clientService);

        oAuth2AuthorizedClientManager.setAuthorizedClientProvider(new ClientCredentialsReactiveOAuth2AuthorizedClientProvider());


        return dbsWebClientConfiguration.dbsWebClient(objectMapper, oAuth2AuthorizedClientManager, builder, new DbsWebClientConfigurationProperties());
    }

    protected ObjectMapper getObjectMapper() {
        return dbsWebClientConfiguration.objectMapper(dbsWebClientConfiguration.dateFormat());
    }

    protected DateFormat getDateFormat() {
        return dbsWebClientConfiguration.dateFormat();
    }

    private static class CustomLogger extends LoggingHandler {

        CustomLogger(Class<?> clazz) {
            super(clazz);
        }

        @Override
        protected String format(ChannelHandlerContext ctx, String event, Object arg) {
            if (arg instanceof ByteBuf) {
                ByteBuf msg = (ByteBuf) arg;
                return decode(
                    msg, msg.readerIndex(), msg.readableBytes(), Charset.defaultCharset());
            }
            return super.format(ctx, event, arg);
        }

        private String decode(ByteBuf src, int readerIndex, int len, Charset charset) {
            if (len != 0) {
                byte[] array;
                int offset;
                if (src.hasArray()) {
                    array = src.array();
                    offset = src.arrayOffset() + readerIndex;
                } else {
                    array = PlatformDependent.allocateUninitializedArray(Math.max(len, 1024));
                    offset = 0;
                    src.getBytes(readerIndex, array, 0, len);
                }
                return new String(array, offset, len, charset);
            }
            return "";
        }

        // further code omitted for brevity
    }

}
