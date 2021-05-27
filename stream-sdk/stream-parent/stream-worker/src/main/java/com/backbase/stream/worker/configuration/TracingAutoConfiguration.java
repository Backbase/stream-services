package com.backbase.stream.worker.configuration;

import brave.Tracer;
import brave.propagation.TraceContext;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.sleuth.autoconfig.instrument.reactor.TraceReactorAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.WebFilter;

@Configuration
@AutoConfigureAfter(TraceReactorAutoConfiguration.class)
public class TracingAutoConfiguration {

    /**
     * Implementing filter to add the tracing headers. Spring Sleuth proposed implementation with
     * HttpServerResponseParser bean didn't work for Reactive apps since ServerHttpResponse headers are read-only.
     *
     * @param tracer Sleuth tracer.
     * @return filter bean.
     * @see <a href="https://cloud.spring.io/spring-cloud-static/spring-cloud-sleuth/2.2.3.RELEASE/reference/html/#tracingfilter">TracingFilter</a>
     */
    @Bean
    @ConditionalOnProperty("backbase.stream.web.trace.response.enabled")
    public WebFilter responseTracingWebFilter(final Tracer tracer) {
        return (exchange, chain) -> {
            if (tracer.currentSpan() != null) {
                TraceContext context = tracer.currentSpan().context();
                HttpHeaders headers = exchange.getResponse().getHeaders();
                headers.add("X-B3-TraceId", context.traceIdString());
                headers.add("X-B3-SpanId", context.spanIdString());
            }
            return chain.filter(exchange);
        };
    }

}
