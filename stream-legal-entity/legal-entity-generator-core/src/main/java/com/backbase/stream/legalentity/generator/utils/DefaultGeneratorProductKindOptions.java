package com.backbase.stream.legalentity.generator.utils;

import com.backbase.stream.product.generator.configuration.ProductGeneratorConfigurationProperties;
import com.backbase.stream.product.generator.configuration.ProductKindGeneratorProperties;
import com.github.javafaker.Country;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.iban4j.CountryCode;

public class DefaultGeneratorProductKindOptions {

    public static final String LOCALE = "EN";
    public static final CountryCode ibanCountryCode = CountryCode.NL;



    public static  ProductGeneratorConfigurationProperties getProductGeneratorConfigurationProperties() {
        return getProductGeneratorConfigurationProperties(LOCALE, ibanCountryCode);
    }

    public static  ProductGeneratorConfigurationProperties getProductGeneratorConfigurationProperties(String locale, CountryCode ibanCountryCode) {
        ProductGeneratorConfigurationProperties options = new ProductGeneratorConfigurationProperties();

        Map<String, ProductKindGeneratorProperties> kinds = new HashMap<>();
        ProductKindGeneratorProperties currentAccountGenerator = new ProductKindGeneratorProperties();
        currentAccountGenerator.setDistribution(atLeastOneMaybeTwo());
        currentAccountGenerator.setGenerateIban(true);
        currentAccountGenerator.setGenerateBBAN(false);
        currentAccountGenerator.setCurrency("EUR");
        currentAccountGenerator.setMaximumAvailableBalance(5000d);
        currentAccountGenerator.setMinimumAvailableBalance(200d);


        currentAccountGenerator.setIbanCountryCode(ibanCountryCode);

        ProductKindGeneratorProperties savingsAccountAccountGenerator = new ProductKindGeneratorProperties();
        savingsAccountAccountGenerator.setDistribution(maybeOne());
        savingsAccountAccountGenerator.setGenerateIban(true);
        savingsAccountAccountGenerator.setGenerateBBAN(false);
        savingsAccountAccountGenerator.setCurrency("EUR");
        savingsAccountAccountGenerator.setMaximumBookedBalance(20000d);
        savingsAccountAccountGenerator.setMinimumBookedBalance(200d);
        savingsAccountAccountGenerator.setIbanCountryCode(ibanCountryCode);

        ProductKindGeneratorProperties creditCardGenerator = new ProductKindGeneratorProperties();
        creditCardGenerator.setDistribution(maybeOne());
        creditCardGenerator.setGenerateIban(false);
        creditCardGenerator.setGenerateBBAN(false);
        creditCardGenerator.setCurrency("EUR");
        creditCardGenerator.setMaximumAvailableBalance(5000d);
        creditCardGenerator.setMinimumAvailableBalance(200d);
        creditCardGenerator.setIbanCountryCode(ibanCountryCode);

        ProductKindGeneratorProperties debitAccount = new ProductKindGeneratorProperties();
        debitAccount.setDistribution(atLeastOneMaybeTwo());
        debitAccount.setGenerateIban(true);
        debitAccount.setGenerateBBAN(true);
        debitAccount.setCurrency("EUR");
        debitAccount.setMaximumAvailableBalance(2000d);
        debitAccount.setMinimumAvailableBalance(0d);
        debitAccount.setIbanCountryCode(ibanCountryCode);


        ProductKindGeneratorProperties loan = new ProductKindGeneratorProperties();
        loan.setDistribution(atLeastOneMaybeTwo());
        loan.setGenerateIban(false);
        loan.setGenerateBBAN(true);
        loan.setCurrency("EUR");
        loan.setMaximumAvailableBalance(2000d);
        loan.setMinimumAvailableBalance(0d);
        loan.setMaximumLimit(5000d);
        loan.setMinimumLimit(4000d);
        loan.setIbanCountryCode(ibanCountryCode);

        ProductKindGeneratorProperties termDeposit = new ProductKindGeneratorProperties();
        termDeposit.setDistribution(atLeastOneMaybeTwo());
        termDeposit.setGenerateIban(false);
        termDeposit.setGenerateBBAN(true);
        termDeposit.setCurrency("EUR");
        termDeposit.setMaximumAvailableBalance(2000d);
        termDeposit.setMinimumAvailableBalance(0d);
        termDeposit.setMaximumLimit(5000d);
        termDeposit.setMinimumLimit(4000d);
        termDeposit.setIbanCountryCode(ibanCountryCode);



        kinds.put("kind1", currentAccountGenerator);
        kinds.put("kind2", savingsAccountAccountGenerator);
        kinds.put("kind3", creditCardGenerator);
        kinds.put("kind4", debitAccount);
        kinds.put("kind6", termDeposit);
        kinds.put("kind7", loan);

        options.setKinds(kinds);

        return options;
    }

    private static  Map<Integer, Double> maybeOne() {
        return Collections.singletonMap(1, 0.5d);
    }

    private static Map<Integer, Double> atLeastOneMaybeTwo() {
        Map<Integer, Double> result = new LinkedHashMap<>();
        result.put(1, 1.0d);
        result.put(2, 0.5d);
        return result;
    }

    private static Map<Integer, Double> atLeastOneMaybeTwoOrThree() {
        Map<Integer, Double> result = new LinkedHashMap<>();
        result.put(1, 1.0d);
        result.put(2, 0.5d);
        result.put(3, 0.3d);
        return result;
    }


}
