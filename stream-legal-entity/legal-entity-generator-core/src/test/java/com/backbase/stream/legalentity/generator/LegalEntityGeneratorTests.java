package com.backbase.stream.legalentity.generator;

import com.backbase.stream.legalentity.generator.configuration.LegalEntityGeneratorConfigurationProperties;
import com.backbase.stream.legalentity.generator.utils.DefaultGeneratorProductKindOptions;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityResponse;
import com.backbase.stream.product.generator.ProductGenerator;
import com.backbase.stream.product.generator.configuration.ProductGeneratorConfigurationProperties;
import com.backbase.stream.product.generator.configuration.ProductKindGeneratorProperties;
import com.backbase.stream.productcatalog.ProductCatalogService;
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
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.iban4j.CountryCode;
import org.junit.Ignore;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Ignore
public class LegalEntityGeneratorTests {

    @Test
    public void testGenerator() throws JsonProcessingException, InterruptedException {

        LegalEntityGeneratorConfigurationProperties legalEntityGeneratorConfigurationProperties = getLegalEntityGeneratorConfigurationProperties();
        ProductGeneratorConfigurationProperties productGeneratorConfigurationProperties = DefaultGeneratorProductKindOptions.getProductGeneratorConfigurationProperties("EN", CountryCode.NL);

        ProductCatalogService productCatalogService = mock(ProductCatalogService.class);
        when(productCatalogService.getProductCatalog()).thenReturn(mockProductCatalog());

        ProductGenerator productGenerator = new ProductGenerator(productGeneratorConfigurationProperties);
        LegalEntityGenerator generator = new LegalEntityGenerator(legalEntityGeneratorConfigurationProperties, productGenerator);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        writeToYaml(legalEntityGeneratorConfigurationProperties);
        writeToYaml(productGeneratorConfigurationProperties);

        int requests = 1;
        int perRequest = RandomUtils.nextInt(1,10);
        execute(generator, requests, perRequest, true);
    }

    private void execute(LegalEntityGenerator generator, int requests, int perRequest, boolean addAsSubsidiaries) throws JsonProcessingException, InterruptedException {
        int counter = 0;
        RestTemplate restTemplate = new RestTemplate();
        while (counter < requests) {
            List<LegalEntity> randomLegalEntities = Stream.generate(() -> generator.generate(mockProductCatalog()))
                .limit(perRequest)
                .collect(Collectors.toList());


//            if (addAsSubsidiaries) {
//                LegalEntity root = new LegalEntity();
//                root.setExternalId(generator.options.getParentLegalEntityId());
//                root.setSubsidiaries(randomLegalEntities);
//                randomLegalEntities = Collections.singletonList(root);
//            }

            writeToJson(randomLegalEntities, new File("target/request.json"));
            Instant instant = Instant.now();
            ResponseEntity<LegalEntityResponse> responseEntity = restTemplate.postForEntity("http://localhost:8080//async/legal-entity", randomLegalEntities, LegalEntityResponse.class);

            writeToJson(responseEntity.getBody(), new File("target/response.json"));
            writeToYaml(responseEntity.getBody());
            log.info("Ingested in: {}ms", Duration.between(instant, Instant.now()).toMillis());
            counter++;
            Thread.sleep(200);
        }
    }

    @Test
    public void testUnhappyGenerator() throws JsonProcessingException, InterruptedException {

        LegalEntityGeneratorConfigurationProperties legalEntityGeneratorConfigurationProperties = getLegalEntityGeneratorConfigurationProperties();
        legalEntityGeneratorConfigurationProperties.setErrorRate(0.0d);

        ProductGeneratorConfigurationProperties productGeneratorConfigurationProperties =  DefaultGeneratorProductKindOptions.getProductGeneratorConfigurationProperties("EN", CountryCode.NL);
        productGeneratorConfigurationProperties.getKinds().values().forEach(productKindGeneratorProperties ->
            productKindGeneratorProperties.setErrorRateUnknownProductId(0.5d));

        ProductCatalogService productCatalogService = mock(ProductCatalogService.class);
        when(productCatalogService.getProductCatalog()).thenReturn(mockProductCatalog());

        ProductGenerator productGenerator = new ProductGenerator(productGeneratorConfigurationProperties);
        LegalEntityGenerator generator = new LegalEntityGenerator(legalEntityGeneratorConfigurationProperties, productGenerator);

        int requests = 1;
        int perRequest = 1;
        execute(generator, requests, perRequest, false);
    }


    private LegalEntityGeneratorConfigurationProperties getLegalEntityGeneratorConfigurationProperties() {
        LegalEntityGeneratorConfigurationProperties options = new LegalEntityGeneratorConfigurationProperties();
        options.setGenerateProducts(true);
        options.setParentLegalEntityId("mock-bank");
        options.setLocale(Locale.forLanguageTag("NL"));
        return options;
    }

    private ProductCatalog mockProductCatalog() {
        // Built in product kinds
        ProductKind currentAccount = new ProductKind()
            .kindName(BackbaseProductKind.CURRENT_ACCOUNT.toString())
            .externalKindId("kind1");
        ProductKind savingsAccount = new ProductKind()
            .kindName(BackbaseProductKind.SAVINGS_ACCOUNT.toString())
            .externalKindId("kind2");
        ProductKind creditCard = new ProductKind()
            .kindName(BackbaseProductKind.CREDIT_CARD.toString())
            .externalKindId("kind3");
        ProductKind debitAccount = new ProductKind()
            .kindName(BackbaseProductKind.DEBIT_CARD.toString())
            .externalKindId("kind4");
        ProductKind investmentAccount = new ProductKind()
            .kindName(BackbaseProductKind.INVESTMENT_ACCOUNT.toString())
            .externalKindId("kind5");
        ProductKind termDeposit = new ProductKind()
            .kindName(BackbaseProductKind.TERM_DEPOSIT.toString())
            .externalKindId("kind6");
        ProductKind loan = new ProductKind()
            .kindName(BackbaseProductKind.LOAN.toString())
            .externalKindId("kind7");

        // Custom Product Kinds
        ProductKind insurance = new ProductKind()
            .kindName("Insurance")
            .externalKindId("insurance")
            .kindUri("insurance");

        ProductCatalog productCatalog = new ProductCatalog();
        productCatalog.addProductKindsItem(currentAccount);
        productCatalog.addProductKindsItem(savingsAccount);
        productCatalog.addProductKindsItem(creditCard);
        productCatalog.addProductKindsItem(debitAccount);
        productCatalog.addProductKindsItem(investmentAccount);
        productCatalog.addProductKindsItem(termDeposit);
        productCatalog.addProductKindsItem(loan);
        productCatalog.addProductKindsItem(insurance);

        // Current Account Types
        productCatalog.addProductTypesItem(createProductType("standard-account", "Standard Account", currentAccount));
        productCatalog.addProductTypesItem(createProductType("deluxe-account", "Deluxe Account", currentAccount));

        // Saving Account Types
        productCatalog
            .addProductTypesItem(createProductType("flexible-savings-account", "Flexible Savings", savingsAccount));
        productCatalog
            .addProductTypesItem(createProductType("children-savings-account", "Children Savings", savingsAccount));

        // Credit Card Types
        productCatalog.addProductTypesItem(createProductType("credit-card", "Credit Card", creditCard));
        productCatalog.addProductTypesItem(createProductType("gold-card", "Gold Card", creditCard));
        productCatalog.addProductTypesItem(createProductType("platinum-cart", "Platinum Card", creditCard));

        // Debit Cards
        productCatalog.addProductTypesItem(createProductType("debit-card", "Debit Card", debitAccount));

        // Loans
        productCatalog.addProductTypesItem(createProductType("personal-loan", "Personal Loan", loan));
        productCatalog.addProductTypesItem(createProductType("flexible-loan", "Flexible Loan", loan));

        // Investment Accounts
        productCatalog.addProductTypesItem(
            createProductType("guided-investment-account", "Guided Investment Account", investmentAccount));
        productCatalog.addProductTypesItem(
            createProductType("self-directed-investment-account", "Self Directed Investment Account",
                investmentAccount));

        // Term Deposit
        productCatalog.addProductTypesItem(createProductType("term-deposit-5", "Term Deposit 5 years", termDeposit));
        productCatalog.addProductTypesItem(createProductType("term-deposit-10", "Term Deposit 10 years", termDeposit));

        // Custom Product Types
        productCatalog.addProductTypesItem(createProductType("travel-insurance", "Travel Insurance", insurance));
        productCatalog.addProductTypesItem(createProductType("home-insurance", "Home Insurance", insurance));

        return productCatalog;

    }

    private ProductType createProductType(String id, String name, ProductKind productKind) {
        return new ProductType()
            .externalProductId(id)
            .externalProductKindId(productKind.getExternalKindId())
//                .productKind(productKind)
            .externalProductTypeId(productKind.getKindName())
            .productTypeName(name);
    }


    private void writeToYaml(Object johnLegalEntity) throws JsonProcessingException {
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


    private void writeToJson(Object johnLegalEntity, File file) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        System.out.println("\n\n");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(johnLegalEntity));
        System.out.println("\n\n");

        if (file != null) {
            try {
                file.getParentFile().mkdirs();
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, johnLegalEntity);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
