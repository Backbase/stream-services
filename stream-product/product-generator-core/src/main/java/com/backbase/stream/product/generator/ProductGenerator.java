package com.backbase.stream.product.generator;


import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.Product;
import com.backbase.stream.legalentity.model.ProductGroup;
import static com.backbase.stream.product.generator.AbstractProductGenerator.getRandomFromList;
import com.backbase.stream.product.generator.configuration.ProductGeneratorConfigurationProperties;
import com.backbase.stream.product.generator.configuration.ProductKindGeneratorProperties;
import com.backbase.stream.product.generator.utils.DistributedRandomNumberGenerator;
import com.backbase.stream.product.mapping.ProductMapper;
import com.backbase.stream.productcatalog.model.BackbaseProductKind;
import com.backbase.stream.productcatalog.model.ProductCatalog;
import com.backbase.stream.productcatalog.model.ProductKind;
import com.backbase.stream.productcatalog.model.ProductType;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.iban4j.CountryCode;
import org.mapstruct.factory.Mappers;

@Slf4j
public class ProductGenerator {

    private final ProductGeneratorConfigurationProperties options;
    private final ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);

    public ProductGenerator(ProductGeneratorConfigurationProperties options) {
        this.options = options;
    }


    public List<ProductGroup> generate(LegalEntity legalEntity, ProductCatalog productCatalog) {

        ProductGroup productGroup = new ProductGroup();
        productGroup.setName("Generated Product Group");
        productGroup.setUsers(legalEntity.getUsers());

        if(options.getKinds() == null) {
            options.setKinds(getDefaults(options.getCountryCode(), options.getDefaultCurrency()));
        }

        Map<ProductKind, List<ProductType>> groupedProductTypesPerKind = productCatalog.getProductTypes()
            .stream()
            .collect(Collectors.groupingBy(productType -> productCatalog.getProductKinds().stream()
                .filter(kind -> kind.getExternalKindId().equals(productType.getExternalProductKindId()))
                .findFirst()
                .orElseThrow(NullPointerException::new)));


        TreeMap<ProductKind, List<ProductType>>  sorterdProductTypes = new TreeMap<>(Comparator.comparing(ProductKind::getExternalKindId));
        sorterdProductTypes.putAll(groupedProductTypesPerKind);

        sorterdProductTypes.forEach(((productKind, productTypes) -> {
            String externalKindId = productKind.getExternalKindId();
            ProductKindGeneratorProperties generatorProperties = options.getKinds().get(externalKindId);

            if (generatorProperties != null) {
                Map<Integer, Double> distribution = generatorProperties.getDistribution();
                int numberOfAccountsToGenerate = generateRandomDistribution(distribution);

                // ID Pattern Generator

                for (int i = 0; i < numberOfAccountsToGenerate; i++) {
                    ProductType productType = getRandomFromList(productTypes);
                    BackbaseProductKind backbaseProductKind;
                    try {
                        backbaseProductKind = BackbaseProductKind.fromValue(productType.getProductKindName());
                    } catch (Exception e) {
                        backbaseProductKind = BackbaseProductKind.CUSTOM;
                    }

                    switch (backbaseProductKind) {
                        case CURRENT_ACCOUNT:
                            productGroup.addCurrentAccountsItem(new CurrentAccountGenerator(generatorProperties).generate(productType, legalEntity));
                            break;
//                        case SAVINGS_ACCOUNT:
//                            productGroup.addSavingAccountsItem(productMapper.toSavingsAccount(product));
//                            break;
//                        case CREDIT_CARD:
//                            product.setCreditCardAccountNumber(RandomCreditCardNumberGenerator.generateMasterCardNumber());
//                            productGroup.addCreditCardsItem(productMapper.toCreditCard(product));
//                            break;
//                        case DEBIT_CARD:
//                            product.setDebitCardsItems(generateRandomDebitCards(legalEntity, product, generatorProperties));
//                            productGroup.addDebitCardsItem(productMapper.toDebitCard(product));
//                            break;
//                        case TERM_DEPOSIT:
//                            product.setMaturityDate(product.getAccountOpeningDate().plusYears(5));
//                            product.setInterestPaymentFrequencyUnit(InterestPaymentFrequencyUnit.WEEKLY);
//                            productGroup.addTermDepositsItem(productMapper.toTermDeposit(product));
//                            break;
//                        case LOAN:
//                            product.setTermUnit(TermUnit.MONTHLY);
//                            product.setAccountInterestRate(BigDecimal.valueOf(RandomUtils.nextDouble(4d,16d)));
//                            if (generatorProperties.getMinimumLimit() != null
//                                && generatorProperties.getMaximumLimit() != null) {
//                                double accountBalance = RandomUtils.nextDouble(generatorProperties.getMinimumLimit(),
//                                    generatorProperties.getMaximumLimit());
//                                product.setPrincipalAmount(new PrincipalAmount()
//                                    .amount(getAmount(accountBalance, generatorProperties.getAmountScale()))
//                                    .currencyCode(generatorProperties.getCurrency()));
//
//                            }
//                            double amount = RandomUtils.nextDouble(0, product.getPrincipalAmount().getAmount().doubleValue());
//                            product.setBookedBalance(new BookedBalance().amount(BigDecimal.valueOf(amount)).currencyCode(generatorProperties.getCurrency()));
//                            product.setCreditLimitExpiryDate(product.getAccountOpeningDate().plusYears(5));
//                            Loan loansItem = productMapper.toLoan(product);
//                            productGroup.addLoansItem(loansItem);
//                            break;
//                        default:
//                            productGroup.addCustomProductsItem(product);
                    }

                }
            }
        }));
        return Collections.singletonList(productGroup);
    }

    private int generateRandomDistribution(Map<Integer, Double> distribution) {
        return new DistributedRandomNumberGenerator(distribution).getDistributedRandomNumber();
    }



    private Map<String, ProductKindGeneratorProperties> getDefaults(CountryCode ibanCountryCode, String currency) {
        Map<String, ProductKindGeneratorProperties> kinds = new HashMap<>();
        ProductKindGeneratorProperties currentAccountGenerator = new ProductKindGeneratorProperties();
        currentAccountGenerator.setDistribution(atLeastOneMaybeTwo());
        currentAccountGenerator.setGenerateIban(true);
        currentAccountGenerator.setGenerateBBAN(false);
        currentAccountGenerator.setCurrency(currency);
        currentAccountGenerator.setMaximumAvailableBalance(5000d);
        currentAccountGenerator.setMinimumAvailableBalance(200d);


        currentAccountGenerator.setIbanCountryCode(ibanCountryCode);

        ProductKindGeneratorProperties savingsAccountAccountGenerator = new ProductKindGeneratorProperties();
        savingsAccountAccountGenerator.setDistribution(maybeOne());
        savingsAccountAccountGenerator.setGenerateIban(true);
        savingsAccountAccountGenerator.setGenerateBBAN(false);
        savingsAccountAccountGenerator.setCurrency(currency);
        savingsAccountAccountGenerator.setMaximumBookedBalance(20000d);
        savingsAccountAccountGenerator.setMinimumBookedBalance(200d);
        savingsAccountAccountGenerator.setIbanCountryCode(ibanCountryCode);

        ProductKindGeneratorProperties creditCardGenerator = new ProductKindGeneratorProperties();
        creditCardGenerator.setDistribution(maybeOne());
        creditCardGenerator.setGenerateIban(false);
        creditCardGenerator.setGenerateBBAN(false);
        creditCardGenerator.setCurrency(currency);
        creditCardGenerator.setMaximumAvailableBalance(5000d);
        creditCardGenerator.setMinimumAvailableBalance(200d);
        creditCardGenerator.setIbanCountryCode(ibanCountryCode);

        ProductKindGeneratorProperties debitAccount = new ProductKindGeneratorProperties();
        debitAccount.setDistribution(atLeastOneMaybeTwo());
        debitAccount.setGenerateIban(true);
        debitAccount.setGenerateBBAN(true);
        debitAccount.setCurrency(currency);
        debitAccount.setMaximumAvailableBalance(2000d);
        debitAccount.setMinimumAvailableBalance(0d);
        debitAccount.setIbanCountryCode(ibanCountryCode);

        kinds.put("kind1", currentAccountGenerator);
        kinds.put("kind2", savingsAccountAccountGenerator);
        kinds.put("kind3", creditCardGenerator);
        kinds.put("kind4", debitAccount);

        return kinds;
    }

    private Map<Integer, Double> maybeOne() {
        return Collections.singletonMap(1, 0.5d);
    }

    private Map<Integer, Double> atLeastOneMaybeTwo() {
        Map<Integer, Double> result = new LinkedHashMap<>();
        result.put(1, 1.0d);
        result.put(2, 0.5d);
        return result;
    }

    private Map<Integer, Double> atLeastOneMaybeTwoOrThree() {
        Map<Integer, Double> result = new LinkedHashMap<>();
        result.put(1, 1.0d);
        result.put(2, 0.5d);
        result.put(3, 0.3d);
        return result;
    }


    public ProductGeneratorConfigurationProperties getOptions() {
        return options;
    }
}
