package com.backbase.stream.webclient.filter;

import static org.mockito.BDDMockito.will;
import static org.mockito.Mockito.doReturn;

import com.backbase.stream.webclient.configuration.DbsWebClientConfigurationProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.test.StepVerifier;

@Disabled
@ExtendWith(MockitoExtension.class)
public class HeadersForwardingClientFilterTest {

    @Mock
    ClientRequest clientRequest;

    @Mock
    ExchangeFunction exchangeFunction;

    @Mock
    ClientResponse clientResponse;

    @Test
    void shouldAddExtraHeaders() {
        doReturn(new HttpHeaders()).when(clientRequest).headers();
        doReturn(new LinkedMultiValueMap()).when(clientRequest).cookies();

//        will(m -> Mono.just(clientResponse)).given(exchangeFunction).exchange(clientRequest);
        will(s -> HttpStatus.OK).given(clientResponse).statusCode();

        MultiValueMap<String, String> headers = new LinkedMultiValueMap();
        headers.add("x-header", "extra-value");

        DbsWebClientConfigurationProperties properties = new DbsWebClientConfigurationProperties();
        properties.setAdditionalHeaders(headers);

        ExchangeFilterFunction underTest = new HeadersForwardingClientFilter(properties);

        StepVerifier.create(underTest.filter(clientRequest, exchangeFunction))
            .expectNext(clientResponse)
            .verifyComplete();
    }

}
