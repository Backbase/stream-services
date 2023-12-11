package com.backbase.stream.context;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * Associates a given tenant with the current execution thread.
 *
 * <p>
 * This class provides a series of static methods that delegate to a
 * <code>InheritableThreadLocal</code>.
 * </P>
 *
 * @see ThreadLocal
 */
@Slf4j
public final class TenantContext {

    /**
     * Key in the logging MDC that corresponds to the bound tenant ID.
     */
    public static final String MDC_TID = "TID";

    private static final ThreadLocal<String> tenant = new InheritableThreadLocal<>();

    /**
     * Hide the implicit public constructor.
     */
    private TenantContext() {
        throw new AssertionError("TenantContext cannot be instantiated.");
    }

    /**
     * Associates a new <code>Tenant</code> with the current thread of execution and the ID of the tenant with the
     * {@link #MDC_TID TID key} in the logging {@link MDC}.
     *
     * @param tenant the new <code>Tenant</code> (may be <code>null</code>)
     */
    public static void setTenant(String tenant) {
        if (tenant == null) {
            log.info("null value provided to setTenant method; no tenant will be bound to the current thread.");
            clear();
        } else {
            TenantContext.tenant.set(tenant);
            MDC.put(MDC_TID, tenant);
        }
    }

    /**
     * Obtain the current <code>Tenant</code>.
     *
     * @return An Optional containing the Tenant or an empty Optional if no tenant found
     */
    public static Optional<String> getTenant() {
        return Optional.ofNullable(tenant.get());
    }

    /**
     * Explicitly clears the context value from the current thread and logging {@link MDC}.
     */
    public static void clear() {
        tenant.remove();
        MDC.remove(MDC_TID);
    }
}
