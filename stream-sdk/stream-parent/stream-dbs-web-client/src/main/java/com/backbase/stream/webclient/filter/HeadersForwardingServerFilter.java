package com.backbase.stream.webclient.filter;

import static com.backbase.stream.webclient.DbsWebClientConfiguration.CONTEXT_KEY_FORWARDED_HEADERS;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class HeadersForwardingServerFilter implements WebFilter {

    private static final List<String> HEADERS_TO_FORWARD = asList("X-TID");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        log.debug("forwarding request headers for: " + Optional.ofNullable(exchange.getRequest())
            .map(ServerHttpRequest::getPath).map(RequestPath::toString).orElse("null"));
        final LinkedMultiValueMap<String, String> forwardedHeaders = new LinkedMultiValueMap<>();
        final ServerHttpRequest request = exchange.getRequest();
        HEADERS_TO_FORWARD.forEach(headerKey -> {
            List<String> headerValues = request.getHeaders().get(headerKey);
            log.debug("forwarding header: {}={}", headerKey, headerValues);
            if (headerValues != null) {
                forwardedHeaders.addAll(headerKey, headerValues);
            }
        });
        return chain.filter(exchange).contextWrite((ctx) -> ctx.put(CONTEXT_KEY_FORWARDED_HEADERS, forwardedHeaders));
    }

}