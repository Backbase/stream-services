package com.backbase.stream.m10y.reactor;

import static com.backbase.stream.m10y.config.MultiTenancyConfigurationProperties.TENANT_HEADER_NAME;

import com.backbase.stream.m10y.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import org.springframework.util.MultiValueMap;
import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

@Slf4j
public class HeaderForwardingContextSubscriber<T> implements CoreSubscriber<T> {

    public static final String FORWARDED_HEADERS_CONTEXT_KEY = "headers";

    private final CoreSubscriber<T> delegate;

    private final Context context;

    public HeaderForwardingContextSubscriber(CoreSubscriber<T> delegate,
        MultiValueMap<String, String> headers) {
        this.delegate = delegate;
        this.context = getOrPutContext(headers, this.delegate.currentContext());
    }

    private Context getOrPutContext(MultiValueMap<String, String> headers, Context currentContext) {
        if (currentContext.hasKey(FORWARDED_HEADERS_CONTEXT_KEY)) {
            return currentContext;
        }
        log.debug("Setting tenant in reactor context: {}", headers.getFirst(TENANT_HEADER_NAME));
        return currentContext.put(FORWARDED_HEADERS_CONTEXT_KEY, headers);
    }

    @Override
    public Context currentContext() {
        return this.context;
    }

    @Override
    public void onSubscribe(Subscription s) {
        this.delegate.onSubscribe(s);
    }

    @Override
    public void onNext(T t) {
        setTenantInThreadContext();
        this.delegate.onNext(t);
    }

    @Override
    public void onError(Throwable ex) {
        this.delegate.onError(ex);
    }

    @Override
    public void onComplete() {
        this.delegate.onComplete();
    }

    /**
     * Set the tenant context in the reactor thread scope. This implementation is safe only for event consumption
     * concurrency set to one (SSDK's default value). 'spring.cloud.stream.default.consumer.concurrency=1'
     */
    private void setTenantInThreadContext() {
        // TODO: Use the new reactor context propagator when upgrading to SSDK 16 to avoid concurrency issues.
        MultiValueMap<String, String> headers = currentContext().get(FORWARDED_HEADERS_CONTEXT_KEY);
        headers.get(TENANT_HEADER_NAME)
            .stream()
            .findFirst()
            .map(tenant -> {
                log.warn(
                    "Setting tenant in thread context: {}, currently only supported for non-concurrent events consumption.",
                    tenant);
                return tenant;
            })
            .ifPresent(TenantContext::setTenant);
    }


}
