package com.backbase.stream.webclient.filter;

import com.backbase.stream.context.ForwardedHeadersAccessor;
import com.backbase.stream.webclient.configuration.DbsWebClientConfigurationProperties;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

@Slf4j
@AllArgsConstructor
public class HeaderForwardingClientFilter implements ExchangeFilterFunction {

    private final DbsWebClientConfigurationProperties properties;

    @Override
    public Mono<ClientResponse> filter(ClientRequest originalRequest, ExchangeFunction next) {
        ClientRequest additionalHeadersRequest = enrichRequestWithAdditionalHeaders(originalRequest);

        return Mono.deferContextual(context -> {
            Optional<HttpHeaders> forwardHeaders = context.getOrEmpty(ForwardedHeadersAccessor.KEY);
            log.trace("Context contains headers? {}", forwardHeaders.isPresent());
            log.trace("Forwarded headers: {}", forwardHeaders.map(MultiValueMap::toString).orElse("none"));

            ClientRequest forwardHeadersRequest = enrichRequestWithForwardedHeaders(additionalHeadersRequest,
                forwardHeaders);

            return next.exchange(forwardHeadersRequest);
        });
    }

    private ClientRequest enrichRequestWithAdditionalHeaders(ClientRequest originalRequest) {
        return Optional.ofNullable(properties.getAdditionalHeaders())
            .map(additionalHeaders -> {
                log.debug("Adding additional headers: {} from configuration to Request: {}", additionalHeaders,
                    originalRequest.url());
                return ClientRequest.from(originalRequest)
                    .headers(httpHeaders -> httpHeaders.addAll(additionalHeaders))
                    .build();
            })
            .orElse(originalRequest);
    }

    private ClientRequest enrichRequestWithForwardedHeaders(ClientRequest additionalHeadersRequest,
        Optional<HttpHeaders> forwardHeaders) {
        return forwardHeaders.map(headers -> {
                log.debug("Adding additional headers: {} from Reactive subscriber context to Request: {}", headers,
                    additionalHeadersRequest.url());
                return ClientRequest.from(additionalHeadersRequest)
                    .headers(httpHeaders -> httpHeaders.putAll(headers))
                    .build();
            })
            .orElse(additionalHeadersRequest);
    }

}
