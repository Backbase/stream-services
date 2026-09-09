package com.backbase.stream.investment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TranslationsSupportTest {

    @Test
    void filterTranslations_passesThroughAllFieldsForNonDefaultLocales() {
        Map<String, Map<String, String>> translations = Map.of(
            "en-gb", Map.of("name", "Aggressive"),
            "fr-ch", Map.of(
                "name", "Agressif",
                "description", "Description FR",
                "badge_text", "Risque 5 sur 5"));

        Map<String, Map<String, String>> filtered = TranslationsSupport.filterTranslations(translations);

        assertThat(filtered).containsOnlyKeys("fr-ch");
        assertThat(filtered.get("fr-ch")).containsEntry("name", "Agressif");
        assertThat(filtered.get("fr-ch")).containsEntry("description", "Description FR");
        assertThat(filtered.get("fr-ch")).containsEntry("badge_text", "Risque 5 sur 5");
    }

    @Test
    void filterTranslations_normalizesLocaleKeys() {
        assertThat(TranslationsSupport.filterTranslations(
            Map.of("FR-CH", Map.of("name", "Agressif")))).containsOnlyKeys("fr-ch");
    }

    @Test
    void filterTranslations_returnsNullWhenNothingToSend() {
        assertThat(TranslationsSupport.filterTranslations(null)).isNull();
        assertThat(TranslationsSupport.filterTranslations(Map.of())).isNull();
        assertThat(TranslationsSupport.filterTranslations(
            Map.of("en-gb", Map.of("name", "Aggressive")))).isNull();
    }

    @Test
    void filterTranslations_skipsBlankValues() {
        Map<String, Map<String, String>> translations = Map.of(
            "fr-ch", new LinkedHashMap<>(Map.of("name", "Agressif", "description", "  ")));

        assertThat(TranslationsSupport.filterTranslations(translations).get("fr-ch"))
            .containsOnlyKeys("name");
    }
}
