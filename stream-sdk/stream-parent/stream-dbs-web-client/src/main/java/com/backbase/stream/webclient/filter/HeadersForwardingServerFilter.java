package com.backbase.stream.webclient.filter;

import static com.backbase.stream.webclient.DbsWebClientConfiguration.CONTEXT_KEY_FORWARDED_HEADERS;

import com.backbase.stream.webclient.configuration.DbsWebClientConfigurationProperties;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class HeadersForwardingServerFilter implements WebFilter {

    private final DbsWebClientConfigurationProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        log.trace("forwarding request headers for: " + Optional.ofNullable(exchange.getRequest())
            .map(ServerHttpRequest::getPath).map(RequestPath::toString).orElse("null"));
        LinkedMultiValueMap<String, String> forwardedHeaders = new LinkedMultiValueMap<>();
        ServerHttpRequest request = exchange.getRequest();
        properties.getHeadersToForward().forEach(headerKey -> {
            List<String> headerValues = request.getHeaders().get(headerKey);
            if (headerValues != null) {
                log.debug("forwarding header: {}={}", headerKey, headerValues);
                forwardedHeaders.addAll(headerKey, headerValues);
            }
        });
        return chain.filter(exchange).contextWrite((ctx) -> ctx.put(CONTEXT_KEY_FORWARDED_HEADERS, forwardedHeaders));
    }

}
