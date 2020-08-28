package com.backbase.stream.legalentity;

import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.legalentity.generator.configuration.LegalEntityGeneratorConfigurationProperties;
import com.backbase.stream.legalentity.model.AvailableBalance;
import com.backbase.stream.legalentity.model.BookedBalance;
import com.backbase.stream.legalentity.model.CreditCard;
import com.backbase.stream.legalentity.model.CreditLimit;
import com.backbase.stream.legalentity.model.CurrentAccount;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityType;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.SavingsAccount;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.productcatalog.model.ProductCatalog;
import com.backbase.stream.productcatalog.model.ProductType;
import com.backbase.stream.worker.model.UnitOfWork;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.javafaker.Address;
import com.github.javafaker.Faker;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.iban4j.CountryCode;
import org.iban4j.Iban;

import static java.util.Arrays.asList;

public class TestUtils {

    public static final String LOCALE = "EN";
    public static final CountryCode ibanCountryCode = CountryCode.NL;


    protected static final List<CountryCode> SEPA_COUNTRY_CODES;


    static {
        List<String> allowed = asList("AT", "BE", "BG", "CH", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GB",
            "GI", "GR", "HR", "HU", "IE", "IS", "IT", "LI", "LT", "LU", "LV", "MC", "MT", "NL", "PL", "PT", "RO", "SE",
            "SI", "SK", "SM");
        SEPA_COUNTRY_CODES = new ArrayList<>();
        CountryCode[] values = CountryCode.values();
        for (CountryCode code : values) {
            if (allowed.contains(code.name())) {
                SEPA_COUNTRY_CODES.add(code);
            }
        }
    }


    protected static void writeToYaml(LegalEntity johnLegalEntity) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper((new YAMLFactory()));
        mapper.registerModule(new JavaTimeModule());
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        System.out.println("\n\n");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(johnLegalEntity));
        System.out.println("\n\n");
    }

    public static void writeToYaml(List<LegalEntity> list) {
        ObjectMapper mapper = new ObjectMapper((new YAMLFactory()));
        mapper.registerModule(new JavaTimeModule());
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        System.out.println("\n\n");
        try {
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(list));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        System.out.println("\n\n");
    }

    protected static String generateRandomIban() {
        return Iban.random(getRandomFromList(SEPA_COUNTRY_CODES)).toString();
    }

    protected static <T> T getRandomFromList(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    public static BigDecimal generateRandomAmountInRange(long min, long max) {
        long clamp = max * 10 - min * 10;
        long value = Math.abs((ThreadLocalRandom.current().nextLong() % clamp));
        return new BigDecimal("" + ((value / 10D) + min)).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public static LegalEntityGeneratorConfigurationProperties getLegalEntityGeneratorConfigurationProperties() {
        LegalEntityGeneratorConfigurationProperties options = new LegalEntityGeneratorConfigurationProperties();
        options.setGenerateProducts(true);
        options.setParentLegalEntityId("mock-bank");
        options.setLocale(Locale.forLanguageTag(LOCALE));
        return options;
    }

    protected static SavingsAccount getSavingsAccount() {
        SavingsAccount savingsAccount = new SavingsAccount();
        savingsAccount
            .productTypeExternalId("flexible-savings-account")
            .externalId("savings-account")
            .name("My Savings Account ")
            .bankAlias("Johns savings Account")
            .crossCurrencyAllowed(true);
        savingsAccount
            .IBAN("NL42INGB4416709382")
            .currency("EUR");
        savingsAccount
            .startDate(OffsetDateTime.now().minus(Period.ofYears(4)))
            .bankBranchCode("ING_AMSTERDAM")
            .accountOpeningDate(OffsetDateTime.now().minus(Period.ofYears(4)));
        savingsAccount
            .accruedInterest(BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(10)));
        savingsAccount
            .bookedBalance(
                new BookedBalance().currencyCode("EUR").amount(generateRandomAmountInRange(10000L, 9999999L)))
            .externalTransferAllowed(true);
        return savingsAccount;
    }

    protected static CurrentAccount getCurrentAccount() {
        CurrentAccount currentAccount = new CurrentAccount();
        currentAccount
            .productTypeExternalId("deluxe-account")
            .externalId("deluxe-account")
            .name("My Deluxe Current Account ")
            .bankAlias("Johns Deluxe Current Account")
            .currency("EUR")
            .accountOpeningDate(OffsetDateTime.now().minus(Period.ofYears(4)))
            .externalTransferAllowed(true)
            .crossCurrencyAllowed(true);

        currentAccount.IBAN("NL44INGB9932384941")
            .urgentTransferAllowed(true)
            .startDate(OffsetDateTime.now().minus(Period.ofYears(4)))
            .bankBranchCode("ING_AMSTERDAM")
            .bookedBalance(new BookedBalance().currencyCode("EUR").amount(generateRandomAmountInRange(10000L, 9999999L)))
            .availableBalance(new AvailableBalance().currencyCode("EUR").amount(generateRandomAmountInRange(1000L, 999999L)))
            .creditLimit(new CreditLimit().currencyCode("EUR").amount(generateRandomAmountInRange(1000L, 999999L)))
            .accruedInterest(BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(10)));
        return currentAccount;
    }

    protected static LegalEntity createRandomLegalEntity(AbstractLegalEntityCoreTests abstractLegalEntityCoreTests) {
        return createRandomLegalEntity(abstractLegalEntityCoreTests, Faker.instance().pokemon().name());
    }

    public static UnitOfWork<LegalEntityTask> getTestUnitOfWork() {
        User tinus = new User().externalId("tinus").fullName("Tinus Test Geval");
        LegalEntity tinusLegalEntity = new LegalEntity().externalId("tinus")
            .name("Tinus Test Geval")
            .addAdministratorsItem(tinus)
            .legalEntityType(LegalEntityType.CUSTOMER)
            .parentExternalId("bank");
        JobProfileUser usersItem = new JobProfileUser()
            .user(tinus);
        tinusLegalEntity.addUsersItem(usersItem);

        CurrentAccount currentAccount = getCurrentAccount();
        SavingsAccount savingsAccount = getSavingsAccount();

        ProductGroup test_product_group = new ProductGroup();
        test_product_group.addCurrentAccountsItem(currentAccount);
        test_product_group.addSavingAccountsItem(savingsAccount);
        test_product_group.setName("Test Product Group");

        tinusLegalEntity.addProductGroupsItem(test_product_group);

        LegalEntityTask task = new LegalEntityTask(tinusLegalEntity);

        return UnitOfWork.from("test", Collections.singletonList(task));
    }

    public static LegalEntity createRandomLegalEntity(AbstractLegalEntityCoreTests abstractLegalEntityCoreTests, String name) {

        User user = new User().externalId(name.toLowerCase()).fullName(name);
        LegalEntity legalEntity = new LegalEntity()
            .name(name + " Legal Entity")
            .externalId(name.toLowerCase())
            .parentExternalId("bank")
            .legalEntityType(LegalEntityType.CUSTOMER)
            .addAdministratorsItem(user);

        legalEntity.addUsersItem(new JobProfileUser().user(user));

        Address address = Faker.instance().address();

        CurrentAccount currentAccount = getCurrentAccount();

        SavingsAccount savingsAccount = getSavingsAccount();


        CreditCard creditCard = getCreditCard(name);

        ProductGroup defaultProductGroup = new ProductGroup();
        defaultProductGroup.addCurrentAccountsItem(currentAccount);
        defaultProductGroup.addCreditCardsItem(creditCard);
        defaultProductGroup.addSavingAccountsItem(savingsAccount);
        legalEntity.addProductGroupsItem(defaultProductGroup);
        return legalEntity;
    }

    protected static CreditCard getCreditCard(String name) {
        CreditCard creditCard = new CreditCard();
        creditCard
            .externalId("gold-card-" + name.toLowerCase())
            .productTypeExternalId("gold-card");
        creditCard
            .creditCardAccountNumber("4593525926904922");
        creditCard
            .name("Gold Card")
            .currency("EUR");
        creditCard
            .bookedBalance(
                new BookedBalance().currencyCode("EUR").amount(generateRandomAmountInRange(10000L, 9999999L)))
            .availableBalance(
                new AvailableBalance().currencyCode("EUR").amount(generateRandomAmountInRange(1000L, 999999L)))
            .creditLimit(new CreditLimit().amount(BigDecimal.valueOf(2000)).currencyCode("EUR"))
            .creditLimitInterestRate(BigDecimal.valueOf(8))
            .cardNumber(BigDecimal.valueOf(4593525926904922d));
        return creditCard;
    }

    protected static String getProductTypeExternalId(ProductCatalog productCatalog, String productKindName) {
        return productCatalog.getProductTypes().stream()
            .filter(productType -> productType.getProductKindName().equals(productKindName)).findFirst()
            .map(ProductType::getExternalProductId).orElseThrow(NullPointerException::new);
    }
}
