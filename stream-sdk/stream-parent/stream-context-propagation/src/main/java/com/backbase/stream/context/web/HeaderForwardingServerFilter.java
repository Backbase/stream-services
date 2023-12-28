package com.backbase.stream.context.web;

import com.backbase.stream.context.ForwardedHeadersAccessor;
import com.backbase.stream.context.config.ContextPropagationConfigurationProperties;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class HeaderForwardingServerFilter implements WebFilter {

    private final ContextPropagationConfigurationProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        log.trace("Forwarding request headers for: {}",
            Optional.ofNullable(exchange.getRequest())
                .map(ServerHttpRequest::getPath)
                .map(RequestPath::toString)
                .orElse("null"));
        HttpHeaders headers =
            assemblyHeadersToForward(properties.getHeadersToForward(), exchange.getRequest().getHeaders());
        return chain.filter(exchange)
            .contextWrite(ctx -> headers.isEmpty() ? ctx : ctx.put(ForwardedHeadersAccessor.KEY, headers));
    }

    private HttpHeaders assemblyHeadersToForward(
        List<String> headersToForward, HttpHeaders requestHeaders) {
        HttpHeaders forwardedHeaders = new HttpHeaders();
        headersToForward.forEach(headerKey -> {
            List<String> headerValues = requestHeaders.get(headerKey);
            if (headerValues != null && !headerValues.isEmpty()) {
                log.debug("Forwarding header: {}={}", headerKey, headerValues);
                forwardedHeaders.addAll(headerKey, headerValues);
            }
        });
        return forwardedHeaders;
    }

}
