package com.backbase.stream.utils;

import com.backbase.accesscontrol.functiongroup.api.service.v1.model.FunctionGroupItem;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Helpers for working with function groups (job roles) returned by Access Control.
 */
@Slf4j
public final class FunctionGroupUtils {

    private FunctionGroupUtils() {
    }

    /**
     * Index the function groups visible to a service agreement by name.
     *
     * <p>Access Control only guarantees name uniqueness within a single service agreement, so the list visible to a
     * service agreement may contain a local (CUSTOM) job role with the same name as a reference job role inherited
     * from the master service agreement, for example after a reference job role was copied in the UI. When names
     * clash, the REFERENCE function group is kept and a warning is logged; otherwise the first one wins.
     *
     * @param functionGroups     function groups visible to the service agreement; may be {@code null}
     * @param serviceAgreementId internal id of the service agreement, used for logging only
     * @return function groups by name, never {@code null}
     */
    public static Map<String, FunctionGroupItem> functionGroupsByName(Collection<FunctionGroupItem> functionGroups,
        String serviceAgreementId) {
        if (functionGroups == null) {
            return Map.of();
        }
        Map<String, List<FunctionGroupItem>> byName = functionGroups.stream()
            .filter(Objects::nonNull)
            .filter(fg -> fg.getName() != null)
            .collect(Collectors.groupingBy(FunctionGroupItem::getName, LinkedHashMap::new, Collectors.toList()));

        Map<String, FunctionGroupItem> result = new LinkedHashMap<>();
        byName.forEach((name, candidates) -> result.put(name, resolve(name, candidates, serviceAgreementId)));
        return result;
    }

    private static FunctionGroupItem resolve(String name, List<FunctionGroupItem> candidates,
        String serviceAgreementId) {
        if (candidates.size() == 1) {
            return candidates.getFirst();
        }
        FunctionGroupItem selected = candidates.stream()
            .filter(fg -> FunctionGroupItem.TypeEnum.REFERENCE.equals(fg.getType()))
            .findFirst()
            .orElse(candidates.getFirst());
        log.warn("Found {} function groups named '{}' visible to service agreement {}: {}. Using {} ({}).",
            candidates.size(), name, serviceAgreementId,
            candidates.stream()
                .map(fg -> fg.getId() + " [" + fg.getType() + ", SA " + fg.getServiceAgreementId() + "]")
                .collect(Collectors.joining(", ")),
            selected.getId(), selected.getType());
        return selected;
    }
}
