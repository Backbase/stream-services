package com.backbase.stream.context;

import io.micrometer.context.ThreadLocalAccessor;
import org.springframework.http.HttpHeaders;

public class ForwardedHeadersAccessor implements ThreadLocalAccessor<HttpHeaders> {

    public static final String KEY = "headers";

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public HttpHeaders getValue() {
        return ForwardedHeadersHolder.getValue();
    }

    @Override
    public void setValue(HttpHeaders value) {
        ForwardedHeadersHolder.setValue(value);
    }

    @Override
    public void setValue() {
        // Ignore
    }
}
