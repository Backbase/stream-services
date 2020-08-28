package com.backbase.stream.product.generator;

import com.backbase.stream.legalentity.model.CurrentAccount;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.product.generator.configuration.ProductKindGeneratorProperties;
import com.backbase.stream.productcatalog.model.ProductType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.apache.commons.lang3.RandomUtils;
import org.iban4j.Iban;

public class CurrentAccountGenerator extends AbstractProductGenerator<CurrentAccount> {

    public CurrentAccountGenerator(ProductKindGeneratorProperties generatorProperties) {
        super(generatorProperties);
    }


    public CurrentAccount generate(ProductType productType, LegalEntity legalEntity) {

        CurrentAccount product = super.generate(new CurrentAccount(), productType);
        Iban iban = Iban.random();

        product.setBookedBalance(randomBookedBalance());
        product.setAvailableBalance(randomAvailableBalance());
        product.setCreditLimit(randomCreditLimit());
        product.setIBAN(getIban(iban));
        product.setBBAN(getBban(iban));
        product.setUrgentTransferAllowed(true);
        product.setBIC(getBIC(iban));
        product.setBankBranchCode(getBankBranchCode(iban));
        product.setAccountInterestRate(BigDecimal.valueOf(RandomUtils.nextDouble(0.01d, 0.07d)));
        product.setValueDateBalance(product.getAvailableBalance().getAmount());
        product.setCreditLimitUsage(getCreditLimitUsage(product));
        product.setCreditLimitInterestRate(BigDecimal.valueOf(RandomUtils.nextDouble(0.08d, 0.16d)));
        product.setCreditLimitExpiryDate(OffsetDateTime.now().plusDays(RandomUtils.nextInt(200, 500)));
        product.setAccruedInterest(new BigDecimal(0));
        product.setDebitCardsItems(generateRandomDebitCards(legalEntity, product));
        product.setAccountHolderName(legalEntity.getAdministrators().get(0).getFullName());
        product.setStartDate(product.getAccountOpeningDate());
        product.setMinimumRequiredBalance(BigDecimal.ZERO);

        product.setAccountHolderAddressLine1(faker.address().streetAddress());
        product.setAccountHolderAddressLine2(faker.address().secondaryAddress());
        product.setAccountHolderStreetName(faker.address().streetName());
        product.setTown(faker.address().city());
        product.setPostCode(faker.address().zipCode());
        product.setCountrySubDivision(iban.getCountryCode().toString());
        product.setCreditAccount(super.hasCreditLimit());
        product.setDebitAccount(!super.hasCreditLimit());
        product.setAccountHolderCountry(faker.address().country());

        return product;
    }

    private BigDecimal getCreditLimitUsage(CurrentAccount product) {
        if (super.hasCreditLimit())
            return randomCreditLimit().getAmount().divideToIntegralValue(product.getAvailableBalance().getAmount());
        return null;
    }

    private String getBankBranchCode(Iban iban) {
        if (super.generatorProperties.isGenerateIban()) {
            return iban.getBankCode();
        }
        return null;
    }

    private String getBIC(Iban iban) {
        if (super.generatorProperties.isGenerateBBAN()) {
            return iban.getBankCode();
        }
        return null;
    }

    private String getIban(Iban iban) {
        if (super.generatorProperties.isGenerateIban()) {
            return iban.toString();
        }
        return null;
    }

    private String getBban(Iban iban) {
        if (super.generatorProperties.isGenerateBBAN()) {
            return iban.getBban();
        }
        return null;
    }
}
