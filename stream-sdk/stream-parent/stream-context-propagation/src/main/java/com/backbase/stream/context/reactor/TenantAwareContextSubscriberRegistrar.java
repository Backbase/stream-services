package com.backbase.stream.context.reactor;

import static com.backbase.stream.context.config.ContextPropagationConfigurationProperties.TENANT_HTTP_HEADER_NAME;
import static com.backbase.stream.context.reactor.HeaderForwardingContextSubscriber.FORWARDED_HEADERS_CONTEXT_KEY;

import com.backbase.stream.context.TenantContext;
import java.util.List;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpHeaders;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;

public class TenantAwareContextSubscriberRegistrar implements InitializingBean, DisposableBean {

    private static final String TENANT_AWARE_CONTEXT_OPERATOR_KEY = "TENANT_AWARE_CONTEXT_OPERATOR";

    @Override
    public void afterPropertiesSet() {
        Function<? super Publisher<Object>, ? extends Publisher<Object>> lifter = Operators
            .liftPublisher((pub, sub) -> createSubscriberIfNecessary(sub));
        Hooks.onLastOperator(TENANT_AWARE_CONTEXT_OPERATOR_KEY, (pub) -> {
            if (TenantContext.getTenant().isEmpty()) {
                return pub;
            }
            return lifter.apply(pub);
        });
    }

    @Override
    public void destroy() {
        Hooks.resetOnLastOperator(TENANT_AWARE_CONTEXT_OPERATOR_KEY);
    }

    <T> CoreSubscriber<T> createSubscriberIfNecessary(CoreSubscriber<T> delegate) {
        if (delegate.currentContext().hasKey(FORWARDED_HEADERS_CONTEXT_KEY)) {
            // Already enriched. No need to create Subscriber so return original
            return delegate;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.put(TENANT_HTTP_HEADER_NAME, List.of(TenantContext.getTenant().get()));
        return new HeaderForwardingContextSubscriber<>(delegate, headers);
    }

}

