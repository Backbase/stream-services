package com.backbase.stream.product.generator;

import com.backbase.stream.legalentity.model.AvailableBalance;
import com.backbase.stream.legalentity.model.BaseProduct;
import com.backbase.stream.legalentity.model.BaseProductState;
import com.backbase.stream.legalentity.model.BookedBalance;
import com.backbase.stream.legalentity.model.CreditLimit;
import com.backbase.stream.legalentity.model.CurrentAccount;
import com.backbase.stream.legalentity.model.DebitCardItem;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.product.generator.configuration.ProductKindGeneratorProperties;
import com.backbase.stream.product.generator.utils.DistributedRandomNumberGenerator;
import com.backbase.stream.product.generator.utils.RandomCreditCardNumberGenerator;
import com.backbase.stream.productcatalog.model.ProductType;
import com.github.javafaker.Faker;
import com.mifmif.common.regex.Generex;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomUtils;

public abstract class AbstractProductGenerator<T extends BaseProduct> {

    protected final ProductKindGeneratorProperties generatorProperties;
    protected final Generex randomIdGenerator;
    protected final Generex randomBBANGenerator;
    protected final Faker faker;

    protected AbstractProductGenerator(ProductKindGeneratorProperties generatorProperties) {
        this.generatorProperties = generatorProperties;
        randomIdGenerator = new Generex(generatorProperties.getIdPattern());
        randomBBANGenerator = new Generex(generatorProperties.getBbanPattern());
        faker = Faker.instance(generatorProperties.getLocale());
    }

    protected T generate(T product, ProductType productType) {

        product.setExternalId(randomIdGenerator.random());
        product.setProductTypeExternalId(getExternalProductType(productType));
        product.setCurrency(generatorProperties.getCurrency());
        product.setName(productType.getProductTypeName());
        product.setExternalTransferAllowed(true);
        product.setCrossCurrencyAllowed(true);
        product.setBankAlias(generateBankAlias());
        product.setSourceId("stream-product-generator");
        product.setAccountOpeningDate(OffsetDateTime.now().minusDays(RandomUtils.nextInt(200, 500)));
        product.setLastUpdateDate(OffsetDateTime.now());
        product.setCurrency(generatorProperties.getCurrency());
        product.setState(null);
        return product;
    }

    protected String generateBankAlias() {
        if (generatorProperties.isGenerateAlias()) {
            return faker.ancient().god();
        }
        return null;
    }

    private String getExternalProductType(ProductType productType) {
        String externalProductId = productType.getExternalProductId();
        if (RandomUtils.nextDouble(0d, 1d) <= generatorProperties.getErrorRateUnknownProductId()) {
            externalProductId = ("RandomProductTypeExteranalId_" + System.currentTimeMillis());
        }
        return externalProductId;
    }

    protected List<DebitCardItem> generateRandomDebitCards(LegalEntity legalEntity, CurrentAccount product) {
        Generex cardIdGenerator = new Generex(generatorProperties.getCardIdPattern());
        int numberOfDebitCardsToGenerate = generateRandomDistribution(generatorProperties.getDebitCardDistribution());
        return Stream.generate(createRandomDebitCard(legalEntity, product, generatorProperties, cardIdGenerator))
            .limit(numberOfDebitCardsToGenerate)

            .collect(Collectors.toList());
    }

    protected int generateRandomDistribution(Map<Integer, Double> distribution) {
        return new DistributedRandomNumberGenerator(distribution).getDistributedRandomNumber();
    }

    protected Supplier<DebitCardItem> createRandomDebitCard(LegalEntity legalEntity, CurrentAccount product, ProductKindGeneratorProperties generatorProperties, Generex cardIdGenerator) {
        return () -> {
            String cardType = getRandomFromList(generatorProperties.getCardTypes());
            String cardNumber;
            if ("MASTERCARD".equals(cardType)) {
                cardNumber = RandomCreditCardNumberGenerator.generateMasterCardNumber();
            } else if ("VISA".equals(cardType)) {
                cardNumber = RandomCreditCardNumberGenerator.generateVisaCardNumber();
            } else {
                cardNumber = product.getBBAN();
            }

            return new DebitCardItem()
                .cardholderName(legalEntity.getAdministrators().get(0).getFullName())
                .cardId(cardIdGenerator.random())
                .expiryDate(OffsetDateTime.now().plusDays(RandomUtils.nextInt(100, 300)).toString())
                .number(RandomCreditCardNumberGenerator.generateMasterCardNumber())
                .cardType(cardType)
                .number(cardNumber);
        };
    }


    protected BigDecimal getAmount(double amount, int scale) {
        return BigDecimal.valueOf(amount).setScale(scale, BigDecimal.ROUND_HALF_UP);
    }

    protected static <T> T getRandomFromList(List<T> list) {

        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    protected AvailableBalance randomAvailableBalance() {
        if (generatorProperties.getMinimumAvailableBalance() != null
            && generatorProperties.getMaximumAvailableBalance() != null) {

            double accountBalance = RandomUtils.nextDouble(generatorProperties.getMinimumAvailableBalance(),
                generatorProperties.getMaximumAvailableBalance());

            return new AvailableBalance()
                .amount(getAmount(accountBalance, generatorProperties.getAmountScale()))
                .currencyCode(generatorProperties.getCurrency());
        }
        return null;
    }

    protected BookedBalance randomBookedBalance() {
        if (generatorProperties.getMinimumBookedBalance() != null
            && generatorProperties.getMaximumBookedBalance() != null) {

            double accountBalance = RandomUtils.nextDouble(generatorProperties.getMinimumAvailableBalance(),
                generatorProperties.getMaximumAvailableBalance());

            return new BookedBalance()
                .amount(getAmount(accountBalance, generatorProperties.getAmountScale()))
                .currencyCode(generatorProperties.getCurrency());
        }
        return null;
    }


    protected CreditLimit randomCreditLimit() {
        if (hasCreditLimit()) {

            double accountBalance = RandomUtils.nextDouble(generatorProperties.getMinimumCreditLimit(),
                generatorProperties.getMaximumCreditLimit());

            return new CreditLimit()
                .amount(getAmount(accountBalance, generatorProperties.getAmountScale()))
                .currencyCode(generatorProperties.getCurrency());
        }
        return null;
    }

    boolean hasCreditLimit() {
        return generatorProperties.getMinimumCreditLimit() != null
            && generatorProperties.getMaximumCreditLimit() != null;
    }


}
