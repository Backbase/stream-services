package com.backbase.stream.portfolio.service;

import com.backbase.stream.portfolio.PortfolioTask;
import com.backbase.stream.portfolio.exceptions.PortfolioBundleException;
import com.backbase.stream.worker.exception.StreamTaskException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import org.springframework.http.HttpRequest;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public final class ReactiveStreamHandler {

    private ReactiveStreamHandler() {
    }

    private static final String FAILED = "failed";

    public static <T> Flux<T> getFluxStream(List<T> task) {
        return Flux.fromStream(Optional.ofNullable(task).orElseGet(List::of).stream());
    }

    @NotNull
    public static <T> Function<PortfolioBundleException, Publisher<? extends T>> error(PortfolioTask task,
        String entity, String operation) {
        return exception -> {
            task.error(entity, operation, FAILED, null, null,
                exception, exception.getHttpResponse(), exception.getMessage());
            return Mono.error(new StreamTaskException(task, exception));
        };
    }

    @NotNull
    static <T> Function<WebClientResponseException, Mono<? extends T>> error(Object entity, String errorMessage) {
        return exception ->
            Mono.error(new PortfolioBundleException(entity, errorMessage, exception));
    }

    static void handleWebClientResponseException(WebClientResponseException webclientResponseException) {
        Objects.requireNonNull(webclientResponseException);
        Optional<HttpRequest> responseExceptionRequest = Optional.ofNullable(webclientResponseException.getRequest());
        log.error("Bad Request: \n[{}]: {}\nResponse: {}",
            responseExceptionRequest.map(HttpRequest::getMethod).orElse(null),
            responseExceptionRequest.map(HttpRequest::getURI).orElse(null),
            webclientResponseException.getResponseBodyAsString());
    }

}
