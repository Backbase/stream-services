package com.backbase.stream.worker.configuration;

import brave.Tracer;
import brave.propagation.TraceContext;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.WebFilter;

@Configuration
public class TracingAutoConfiguration {

    @Bean
    @ConditionalOnProperty("backbase.stream.web.trace.response.enabled")
    public WebFilter responseTracingWebFilter(Optional<Tracer> tracer) {
        return (exchange, chain) -> {
            tracer.ifPresent(t -> {
                if (t.currentSpan() != null) {
                    TraceContext context = t.currentSpan().context();
                    HttpHeaders headers = exchange.getResponse().getHeaders();
                    headers.add("X-B3-TraceId", context.traceIdString());
                    headers.add("X-B3-SpanId", context.spanIdString());
                    headers.add("X-B3-ParentSpanId", context.parentIdString());
                }
            });
            return chain.filter(exchange);
        };
    }

}
