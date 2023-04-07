package com.backbase.stream;

import static org.mockito.Mockito.lenient;

import java.net.URI;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

public class WebClientTestUtils {

  private WebClientTestUtils() {}

  public static <T> Mono<T> buildWebResponseExceptionMono(
      Class<? extends WebClientResponseException> exType, HttpMethod httpMethod) {
    WebClientResponseException ex = buildWebClientResponseException(exType, httpMethod);
    return Mono.from(
        (Subscriber<? super T> s) -> {
          throw ex;
        });
  }

  private static WebClientResponseException buildWebClientResponseException(
      Class<? extends WebClientResponseException> exType, HttpMethod method) {
    WebClientResponseException ex = Mockito.mock(exType);
    HttpRequest httpRequest = Mockito.mock(HttpRequest.class);
    lenient().when(httpRequest.getMethod()).thenReturn(method);
    lenient().when(httpRequest.getURI()).thenReturn(URI.create("/some/uri"));
    lenient().when(ex.getRequest()).thenReturn(httpRequest);
    return ex;
  }
}
