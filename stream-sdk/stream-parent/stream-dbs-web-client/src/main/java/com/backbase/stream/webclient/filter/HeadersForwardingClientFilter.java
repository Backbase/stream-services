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

    private final DbsWebClientConfigurationProperties dbsWebClientConfigurationProperties;

    @Override
    public Mono<ClientResponse> filter(ClientRequest clientRequest, ExchangeFunction exchangeFunction) {

        final ClientRequest newRequest = Optional.ofNullable(dbsWebClientConfigurationProperties.getAdditionalHeaders())
            .map(additionalHeaders -> {
                log.debug("Adding additional headers: {} from configuration  Request: {}", additionalHeaders,
                    clientRequest.url());
                return ClientRequest.from(clientRequest)
                    .headers(httpHeaders -> httpHeaders.addAll(additionalHeaders))
                    .build();
            })
            .orElse(clientRequest);

        return Mono.subscriberContext().flatMap(context -> {
            Optional<MultiValueMap<String, String>> forwardHeaders =
                context.<MultiValueMap<String, String>>getOrEmpty(CONTEXT_KEY_FORWARDED_HEADERS);
            log.debug("context contains headers? " + forwardHeaders.isPresent());
            log.debug("forward headers:" + forwardHeaders.map(MultiValueMap::toString).orElse("null"));
            ClientRequest contextRequest = context.<MultiValueMap<String, String>>getOrEmpty("headers")
                .map(headers -> {
                    log.debug("Adding additional headers: {} from Reactive subscriber context to Request: {}", headers,
                        clientRequest.url());
                    return ClientRequest.from(newRequest)
                        .headers(httpHeaders -> httpHeaders.addAll(headers))
                        .build();
                })
                .orElse(newRequest);

            return exchangeFunction.exchange(contextRequest);
        });
    }

}
