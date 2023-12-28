package com.backbase.stream.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;

/**
 * Associates headers to forward with the current execution thread.
 *
 * <p>
 * This class provides a series of static methods that delegate to a
 * <code>InheritableThreadLocal</code>.
 * </P>
 *
 * @see ThreadLocal
 */
@Slf4j
public final class ForwardedHeadersHolder {

    private static final ThreadLocal<HttpHeaders> headers = new InheritableThreadLocal<>();

    private ForwardedHeadersHolder() {
        throw new AssertionError("ForwardedHeadersHolder cannot be instantiated.");
    }

    public static void setValue(HttpHeaders value) {
        if (value == null) {
            log.info("null value provided to setValue method; no value will be bound to the current thread.");
            reset();
        } else {
            headers.set(value);
        }
    }

    public static HttpHeaders getValue() {
        return headers.get();
    }

    public static void reset() {
        headers.remove();
    }
}
