package com.backbase.stream.investment;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Prepares locale-keyed translation maps for the investment integration API.
 *
 * <p>Excludes the default locale ({@code en-gb}) because primary field values are sent separately.
 */
public final class TranslationsSupport {

    private static final String DEFAULT_LOCALE = "en-gb";

    private TranslationsSupport() {
    }

    /**
     * Returns translations ready for the API: non-default locales, blank values removed.
     */
    public static Map<String, Map<String, String>> filterTranslations(
        Map<String, Map<String, String>> translations) {
        if (translations == null || translations.isEmpty()) {
            return null;
        }
        Map<String, Map<String, String>> filtered = new LinkedHashMap<>();
        translations.forEach((locale, fields) -> {
            if (locale == null || fields == null || fields.isEmpty()) {
                return;
            }
            if (DEFAULT_LOCALE.equalsIgnoreCase(locale)) {
                return;
            }
            Map<String, String> filteredFields = new LinkedHashMap<>();
            fields.forEach((field, value) -> {
                if (field != null && value != null && !value.isBlank()) {
                    filteredFields.put(field, value);
                }
            });
            if (!filteredFields.isEmpty()) {
                filtered.put(locale.toLowerCase(), filteredFields);
            }
        });
        return filtered.isEmpty() ? null : filtered;
    }
}
