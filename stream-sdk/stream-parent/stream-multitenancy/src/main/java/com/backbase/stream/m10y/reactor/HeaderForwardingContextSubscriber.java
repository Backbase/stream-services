package com.backbase.stream.m10y.reactor;

import org.reactivestreams.Subscription;
import org.springframework.util.MultiValueMap;
import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

public class HeaderForwardingContextSubscriber<T> implements CoreSubscriber<T> {

    public static final String FORWARDED_HEADERS_CONTEXT_KEY = "headers";

    private final CoreSubscriber<T> delegate;

    private final Context context;

    public HeaderForwardingContextSubscriber(CoreSubscriber<T> delegate,
        MultiValueMap<String, String> attributes) {
        this.delegate = delegate;
        this.context = getOrPutContext(attributes, this.delegate.currentContext());
    }

    private Context getOrPutContext(MultiValueMap<String, String> attributes, Context currentContext) {
        if (currentContext.hasKey(FORWARDED_HEADERS_CONTEXT_KEY)) {
            return currentContext;
        }
        return currentContext.put(FORWARDED_HEADERS_CONTEXT_KEY, attributes);
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

}
