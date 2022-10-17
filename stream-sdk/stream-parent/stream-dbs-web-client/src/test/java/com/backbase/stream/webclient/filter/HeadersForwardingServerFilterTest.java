package com.backbase.stream.webclient.filter;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.backbase.stream.webclient.configuration.DbsWebClientConfigurationProperties;
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

@ExtendWith(MockitoExtension.class)
class HeadersForwardingServerFilterTest {

    @InjectMocks
    private HeadersForwardingServerFilter subject;

    @Mock
    private DbsWebClientConfigurationProperties dbsWebClientConfigurationProperties;

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
        String headerValueToForward = "tenant1";
        LinkedMultiValueMap<String, String> expectedForwardedHeaders = new LinkedMultiValueMap<>();
        expectedForwardedHeaders.put(headerKeyToForward, asList(headerValueToForward));
        Mono<Void> contextAssertionMono = buildContextAssertionMono("headers", expectedForwardedHeaders);

        when(dbsWebClientConfigurationProperties.getHeadersToForward()).thenReturn(asList(headerKeyToForward));
        when(serverWebExchange.getRequest()).thenReturn(serverHttpRequest);
        when(serverHttpRequest.getPath()).thenReturn(requestPath);
        when(serverHttpRequest.getHeaders()).thenReturn(httpHeaders);
        when(httpHeaders.get(headerKeyToForward)).thenReturn(asList(headerValueToForward));
        when(webFilterChain.filter(eq(serverWebExchange))).thenReturn(contextAssertionMono);

        Mono<Void> result = subject.filter(serverWebExchange, webFilterChain);
        result.block();
    }

    @Test
    void filterShouldNotForwardRequestHeaders() {
        Mono<Void> contextAssertionMono = Mono.deferContextual(cv -> {
            var actual = cv.getOrEmpty("headers");
            assertTrue(actual.isEmpty(), "Headers context should be empty");
            return Mono.empty().then();
        });
        when(dbsWebClientConfigurationProperties.getHeadersToForward()).thenReturn(asList("X-TID"));
        when(serverWebExchange.getRequest()).thenReturn(serverHttpRequest);
        when(serverHttpRequest.getPath()).thenReturn(requestPath);
        when(serverHttpRequest.getHeaders()).thenReturn(httpHeaders);
        when(webFilterChain.filter(eq(serverWebExchange))).thenReturn(contextAssertionMono);

        Mono<Void> result = subject.filter(serverWebExchange, webFilterChain);
        result.block();
    }

    private Mono<Void> buildContextAssertionMono(String expectedKey, Object expectedValue) {
        return Mono.deferContextual(cv -> {
            Object actual = cv.get(expectedKey);
            assertEquals(expectedValue, actual,
                String.format("For key '%s' the expected value wasn't provided", expectedKey));
            return Mono.empty().then();
        });
    }
}
