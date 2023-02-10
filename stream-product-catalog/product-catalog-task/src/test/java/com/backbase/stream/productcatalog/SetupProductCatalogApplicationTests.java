package com.backbase.stream.productcatalog;

import com.backbase.stream.productcatalog.model.BackbaseProductKind;
import com.backbase.stream.productcatalog.model.ProductCatalog;
import com.backbase.stream.productcatalog.model.ProductKind;
import com.backbase.stream.productcatalog.model.ProductType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class SetupProductCatalogApplicationTests {


    @Test
    public void testRepository() throws JsonProcessingException {
        ProductKind currentAccount = new ProductKind().kindName(BackbaseProductKind.CURRENT_ACCOUNT.toString())
            .externalKindId("kind1");
        ProductKind savingsAccount = new ProductKind().kindName(BackbaseProductKind.SAVINGS_ACCOUNT.toString())
            .externalKindId("kind2");
        ProductKind creditCard = new ProductKind().kindName(BackbaseProductKind.CREDIT_CARD.toString())
            .externalKindId("kind3");
        ProductKind debitAccount = new ProductKind().kindName(BackbaseProductKind.DEBIT_CARD.toString())
            .externalKindId("kind4");
        ProductKind investmentAccount = new ProductKind()
            .kindName(BackbaseProductKind.INVESTMENT_ACCOUNT.toString())
            .externalKindId("kind5");
        ProductKind termDeposit = new ProductKind().kindName(BackbaseProductKind.TERM_DEPOSIT.toString())
            .externalKindId("kind6");
        ProductKind loan = new ProductKind().kindName(BackbaseProductKind.LOAN.toString())
            .externalKindId("kind7");

        ProductKind insurance = new ProductKind()
            .kindName("Insurance").externalKindId("insurance").kindUri("insurance");

        ProductCatalog productCatalog = new ProductCatalog();
        productCatalog.addProductKindsItem(insurance);

        // Current Account Types
        productCatalog.addProductTypesItem(
            createProductType("standard-account", "Standard Account", currentAccount));
        productCatalog.addProductTypesItem(
            createProductType("deluxe-account", "Deluxe Account", currentAccount));

        // Saving Account Types
        productCatalog.addProductTypesItem(
            createProductType("flexible-savings-account", "Flexible Savings", savingsAccount));
        productCatalog.addProductTypesItem(
            createProductType("children-savings-account", "Children Savings", savingsAccount));

        // Credit Card Types
        productCatalog.addProductTypesItem(
            createProductType("credit-card", "Credit Card", creditCard));
        productCatalog.addProductTypesItem(
            createProductType("gold-card", "Gold Card", creditCard));
        productCatalog.addProductTypesItem(
            createProductType("platinum-cart", "Platinum Card", creditCard));

        // Debit Cards
        productCatalog.addProductTypesItem(
            createProductType("debit-card", "Debit Card", debitAccount));

        // Loans
        productCatalog.addProductTypesItem(
            createProductType("personal-loan", "Personal Loan", loan));
        productCatalog.addProductTypesItem(
            createProductType("flexible-loan", "Flexible Loan", loan));

        // Investment Accounts
        productCatalog.addProductTypesItem(
            createProductType("guided-investment-account", "Guided Investment Account",
                investmentAccount));
        productCatalog.addProductTypesItem(
            createProductType("self-directed-investment-account", "Self Directed Investment Account",
                investmentAccount));

        // Term Deposit
        productCatalog.addProductTypesItem(createProductType("term-deposit-5", "Term Deposit 5 years",
            termDeposit));
        productCatalog.addProductTypesItem(createProductType("term-deposit-10", "Term Deposit 10 years",
            termDeposit));

        // Custom Product Types
        productCatalog.addProductTypesItem(createProductType("travel-insurance", "Travel Insurance",
            insurance));
        productCatalog.addProductTypesItem(createProductType("home-insurance", "Home Insurance",
            insurance));

        ObjectMapper mapper = new ObjectMapper((new YAMLFactory()));
        mapper.registerModule(new JavaTimeModule());
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(productCatalog));
//            productCatalogService.setupProductCatalog(productCatalog);


    }

    private ProductType createProductType(String id, String name, ProductKind productKind) {
        return new ProductType()
            .externalProductId(id)
            .externalProductKindId(productKind.getExternalKindId())
            .externalProductTypeId(productKind.getKindName())
            .productTypeName(name);
    }
}
