package com.backbase.stream.context.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.backbase.stream.context.config.ContextPropagationConfigurationProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class HeaderForwardingServerFilterTest {

    @InjectMocks
    private HeaderForwardingServerFilter subject;

    @Mock
    private ContextPropagationConfigurationProperties contextPropagationConfigurationProperties;

    @Mock
    private ServerWebExchange serverWebExchange;

    @Mock
    private WebFilterChain webFilterChain;

    @Mock
    private ServerHttpRequest serverHttpRequest;

    @Mock
    private RequestPath requestPath;

    @Mock
    private HttpHeaders httpHeaders;

    @Test
    void filterShouldForwardRequestHeaders() {
        String headerKeyToForward = "X-TID";
        List<String> headerValueToForward = List.of("tenant1");
        LinkedMultiValueMap<String, String> expectedForwardedHeaders = new LinkedMultiValueMap<>();
        expectedForwardedHeaders.put(headerKeyToForward, headerValueToForward);

        when(contextPropagationConfigurationProperties.getHeadersToForward()).thenReturn(List.of(headerKeyToForward));
        when(serverWebExchange.getRequest()).thenReturn(serverHttpRequest);
        when(serverHttpRequest.getPath()).thenReturn(requestPath);
        when(serverHttpRequest.getHeaders()).thenReturn(httpHeaders);
        when(webFilterChain.filter(eq(serverWebExchange))).thenReturn(Mono.empty());

        when(httpHeaders.get(headerKeyToForward)).thenReturn(headerValueToForward);

        StepVerifier.create(subject.filter(serverWebExchange, webFilterChain))
            .expectAccessibleContext().contains("headers", expectedForwardedHeaders)
            .then()
            .verifyComplete();
    }

    @Test
    void filterShouldNotForwardRequestHeaders() {
        String headerKeyToForward = "X-TID";

        when(contextPropagationConfigurationProperties.getHeadersToForward()).thenReturn(List.of(headerKeyToForward));
        when(serverWebExchange.getRequest()).thenReturn(serverHttpRequest);
        when(serverHttpRequest.getPath()).thenReturn(requestPath);
        when(serverHttpRequest.getHeaders()).thenReturn(httpHeaders);
        when(webFilterChain.filter(eq(serverWebExchange))).thenReturn(Mono.empty());

        StepVerifier.create(subject.filter(serverWebExchange, webFilterChain))
            .expectAccessibleContext().matches(c -> c.getOrEmpty("headers").isEmpty())
            .then()
            .verifyComplete();
    }
}
