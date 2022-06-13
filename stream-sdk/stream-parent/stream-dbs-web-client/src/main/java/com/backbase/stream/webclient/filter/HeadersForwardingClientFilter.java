package com.backbase.stream.webclient.filter;

import static com.backbase.stream.webclient.DbsWebClientConfiguration.CONTEXT_KEY_FORWARDED_HEADERS;

import com.backbase.stream.webclient.configuration.DbsWebClientConfigurationProperties;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

@Slf4j
@AllArgsConstructor
public class HeadersForwardingClientFilter implements ExchangeFilterFunction {

    private final DbsWebClientConfigurationProperties properties;

    @Override
    public Mono<ClientResponse> filter(ClientRequest originalRequest, ExchangeFunction next) {
        ClientRequest additionalHeadersRequest = enrichRequestWithAdditionalHeaders(originalRequest);

        return Mono.subscriberContext().flatMap(context -> {
            Optional<MultiValueMap<String, String>> forwardHeaders = context.getOrEmpty(CONTEXT_KEY_FORWARDED_HEADERS);
            log.debug("Context contains headers? {}", forwardHeaders.isPresent());
            log.debug("Forwarded headers: {}", forwardHeaders.map(MultiValueMap::toString).orElse("none"));

            ClientRequest forwardHeadersRequest = enrichRequestWithForwardedHeaders(additionalHeadersRequest,
                forwardHeaders);

            return next.exchange(forwardHeadersRequest);
        });
    }

    private ClientRequest enrichRequestWithAdditionalHeaders(ClientRequest originalRequest) {
        ClientRequest additionalHeadersRequest = Optional.ofNullable(properties.getAdditionalHeaders())
            .map(additionalHeaders -> {
                log.debug("Adding additional headers: {} from configuration  Request: {}", additionalHeaders,
                    originalRequest.url());
                return ClientRequest.from(originalRequest)
                    .headers(httpHeaders -> httpHeaders.addAll(additionalHeaders))
                    .build();
            })
            .orElse(originalRequest);
        return additionalHeadersRequest;
    }

    private ClientRequest enrichRequestWithForwardedHeaders(ClientRequest additionalHeadersRequest,
        Optional<MultiValueMap<String, String>> forwardHeaders) {
        ClientRequest forwardHeadersRequest = forwardHeaders.map(headers -> {
                log.debug("Adding additional headers: {} from Reactive subscriber context to Request: {}", headers,
                    additionalHeadersRequest.url());
                return ClientRequest.from(additionalHeadersRequest)
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .build();
            })
            .orElse(additionalHeadersRequest);
        return forwardHeadersRequest;
    }

}
