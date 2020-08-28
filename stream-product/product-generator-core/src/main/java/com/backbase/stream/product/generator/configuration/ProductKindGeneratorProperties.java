package com.backbase.stream.product.generator.configuration;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.iban4j.CountryCode;

@Data
@NoArgsConstructor
public class ProductKindGeneratorProperties {

    private Map<Integer, Double> distribution = Collections.singletonMap(1, 1d);

    private int amountScale = 2;

    private boolean generateAlias = false;

    private Double minimumAccountBalance;

    private Double maximumAccountBalance;

    private Double minimumBookedBalance;

    private Double maximumBookedBalance;

    private Double minimumAvailableBalance;

    private Double maximumAvailableBalance;

    private Double minimumLimit;

    private Double maximumLimit;

    private Double minimumCreditLimit;

    private Double maximumCreditLimit;

    private String currency;

    private boolean generateIban;

    private boolean generateBBAN;

    private boolean isAccountBalanceAsBookedBalance = true;

    private Map<Integer, Double> debitCardDistribution = Collections.singletonMap(1, 1d);

    private CountryCode ibanCountryCode;

    private String bbanPattern = "^[A-Z]{4}\\d{10}";

    private String cardIdPattern = "^[A-Z]{3}\\d{2}";

    private List<String> cardTypes = Stream.of("VISA", "MASTERCARD", "MAESTRO").collect(Collectors.toList());

    private Double errorRateUnknownProductId = 0d;

    private Double errorRateEmptyLegalEntity = 0d;

    private Double errorRateEmptyAccountBalance = 0d;


    /**
     * Regular Expression generated ID's should adhere too. In this example it's 5 capitals with 2 numberic characters
     */
    private String idPattern = "[A-Z]{5}[1-9]{2}";
    private Locale locale = Locale.ENGLISH;


}
