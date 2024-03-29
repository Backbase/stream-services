package com.backbase.stream.webclient.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.will;
import static org.mockito.Mockito.doReturn;

import com.backbase.stream.webclient.configuration.DbsWebClientConfigurationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

@ExtendWith(MockitoExtension.class)
public class HeaderForwardingClientFilterTest {

    @Mock
    ClientRequest clientRequest;

    @Mock
    ExchangeFunction exchangeFunction;

    @Test
    void shouldEnrichWithAdditionalHeaders() {
        doReturn(new HttpHeaders()).when(clientRequest).headers();
        doReturn(new LinkedMultiValueMap()).when(clientRequest).cookies();

        will(a -> assertClientRequestHeader(a.getArgument(0), "X-HEADER"))
            .given(exchangeFunction).exchange(any());

        MultiValueMap<String, String> headersToBeIncluded = new LinkedMultiValueMap();
        headersToBeIncluded.add("x-header", "extra-value");

        DbsWebClientConfigurationProperties properties = new DbsWebClientConfigurationProperties();
        properties.setAdditionalHeaders(headersToBeIncluded);
        ExchangeFilterFunction underTest = new HeaderForwardingClientFilter(properties);

        StepVerifier.create(underTest.filter(clientRequest, exchangeFunction))
            .verifyComplete();
    }

    @Test
    void shouldEnrichWithServerHeadersToForward() {
        doReturn(new HttpHeaders()).when(clientRequest).headers();
        doReturn(new LinkedMultiValueMap()).when(clientRequest).cookies();

        will(a -> assertClientRequestHeader(a.getArgument(0), "x-tid"))
            .given(exchangeFunction).exchange(any());

        var serverRequestHeaders = new HttpHeaders();
        serverRequestHeaders.add("X-TID", "tenant1");

        ExchangeFilterFunction underTest = new HeaderForwardingClientFilter(new DbsWebClientConfigurationProperties());

        StepVerifier.create(underTest.filter(clientRequest, exchangeFunction)
                .contextWrite(Context.of("headers", serverRequestHeaders)))
            .verifyComplete();
    }

    @Test
    void shouldSkipAdditionalHeaderWhenEnrichingWithSameServerHeadersToForward() {
        doReturn(new HttpHeaders()).when(clientRequest).headers();
        doReturn(new LinkedMultiValueMap()).when(clientRequest).cookies();

        will(a -> assertClientRequestHeader(a.getArgument(0), "x-tid", "tenant1"))
            .given(exchangeFunction).exchange(any());

        MultiValueMap<String, String> headersToBeIncluded = new LinkedMultiValueMap();
        headersToBeIncluded.add("x-tid", "tenant2");

        DbsWebClientConfigurationProperties properties = new DbsWebClientConfigurationProperties();
        properties.setAdditionalHeaders(headersToBeIncluded);
        ExchangeFilterFunction underTest = new HeaderForwardingClientFilter(properties);

        var serverRequestHeaders = new HttpHeaders();
        serverRequestHeaders.add("X-TID", "tenant1");

        StepVerifier.create(underTest.filter(clientRequest, exchangeFunction)
                .contextWrite(Context.of("headers", serverRequestHeaders)))
            .verifyComplete();
    }

    /**
     * Given the nature of reactive applications we are asserting when the exchangeFunction is activated, where we
     * validate the processed client request after the header enrichment.
     *
     * @param request     Captured client request after enrichment.
     * @param expectedKey Expected header key.
     * @return Empty mono given we don't actually validate the response here.
     */
    private Mono<Void> assertClientRequestHeader(ClientRequest request, String expectedKey) {
        Assert.notNull(request, "Invalid request");
        Assert.isTrue(request.headers().containsKey(expectedKey), "Missing header");
        return Mono.empty();
    }

    private Mono<Void> assertClientRequestHeader(ClientRequest request, String expectedKey, String expectedValue) {
        Assert.notNull(request, "Invalid request");
        Assert.isTrue(request.headers().get(expectedKey).stream().allMatch(v -> v.equals(expectedValue)),
            "Wrong header value");
        return Mono.empty();
    }


}
