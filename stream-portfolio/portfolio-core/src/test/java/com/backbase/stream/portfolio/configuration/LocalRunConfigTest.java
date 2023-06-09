package com.backbase.stream.portfolio.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;

@ExtendWith(MockitoExtension.class)
class LocalRunConfigTest {

    @InjectMocks
    private LocalRunConfig localRunConfig;

    @Test
    void objectMapper() {
        ObjectMapper mapper = localRunConfig.objectMapper(DateFormat.getInstance());
        Assertions.assertNotNull(mapper);
    }

    @Test
    void dateFormat() {
        DateFormat dateFormat = localRunConfig.dateFormat();
        Assertions.assertNotNull(dateFormat);
    }

    @Test
    void interServiceWebClient() {
        WebClient mockWebClient = Mockito.mock(WebClient.class);

        ObjectProvider interServiceWebClientCustomizers = Mockito.mock(ObjectProvider.class);
        Builder builder = Mockito.mock(Builder.class);

        Mockito.when(builder.build()).thenReturn(mockWebClient);
        Mockito.when(interServiceWebClientCustomizers.orderedStream()).thenReturn(Stream.empty());

        WebClient webClient =
            localRunConfig.interServiceWebClient(interServiceWebClientCustomizers, builder);
        Assertions.assertNotNull(webClient);
        Assertions.assertSame(mockWebClient, webClient);
    }
}
