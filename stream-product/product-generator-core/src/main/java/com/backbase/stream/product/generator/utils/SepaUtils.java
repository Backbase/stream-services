package com.backbase.stream.product.generator.utils;

import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.List;
import org.iban4j.CountryCode;

public class SepaUtils {

    private static List<CountryCode> sepaCountryCodes;

    public static List<CountryCode> getSepaCountryCodes() {
        if (sepaCountryCodes == null) {
            List<String> allowed = asList("AT", "BE", "BG", "CH", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GB",
                "GI", "GR", "HR", "HU", "IE", "IS", "IT", "LI", "LT", "LU", "LV", "MC", "MT", "NL", "PL", "PT", "RO", "SE",
                "SI", "SK", "SM");
            sepaCountryCodes = new ArrayList<>();
            CountryCode[] values = CountryCode.values();
            for (CountryCode code : values) {
                if (allowed.contains(code.name())) {
                    sepaCountryCodes.add(code);
                }
            }
        }
        return sepaCountryCodes;
    }
}
