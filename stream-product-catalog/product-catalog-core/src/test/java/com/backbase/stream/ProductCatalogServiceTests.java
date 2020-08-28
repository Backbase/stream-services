package com.backbase.stream;

import com.backbase.stream.productcatalog.ReactiveProductCatalogService;
import com.backbase.stream.productcatalog.model.BackbaseProductKind;
import com.backbase.stream.productcatalog.model.ProductCatalog;
import com.backbase.stream.productcatalog.model.ProductKind;
import com.backbase.stream.productcatalog.model.ProductType;
import java.io.IOException;
import java.util.logging.Level;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Slf4j
@Ignore
public class ProductCatalogServiceTests extends AbstractServiceIntegrationTests {

    private ReactiveProductCatalogService productCatalogService;

    @Before
    public void setup() {
        String tokenUri = "https://stream-demo.proto.backbasecloud.com/api/token-converter/oauth/token";
        WebClient webClient = super.setupWebClientBuilder(tokenUri, "bb-client", "bb-secret");

        productCatalogService = new ReactiveProductCatalogService(getAccountsPresentationClient(webClient));
    }

    @Test
    public void getGetProductCatalog() {
        Mono<ProductCatalog> productCatalog = productCatalogService.getProductCatalog();

        productCatalog.log("Product Catalog", Level.INFO, true);
        productCatalog.doOnNext(productCatalog1 -> {
            log.info("Product Catalog: {}", productCatalog);
        });

        StepVerifier.create(productCatalog)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    public void testSetupProductCatalog() throws IOException {
        ProductKind currentAccount = new ProductKind().kindName(BackbaseProductKind.CURRENT_ACCOUNT.toString())
            .externalKindId("kind1");
        ProductKind savingsAccount = new ProductKind().kindName(BackbaseProductKind.SAVINGS_ACCOUNT.toString())
            .externalKindId("kind2");
        ProductKind creditCard = new ProductKind().kindName(BackbaseProductKind.CREDIT_CARD.toString())
            .externalKindId("kind3");
        ProductKind debitAccount = new ProductKind().kindName(BackbaseProductKind.DEBIT_CARD.toString())
            .externalKindId("kind4");
        ProductKind investmentAccount = new ProductKind().kindName(BackbaseProductKind.INVESTMENT_ACCOUNT.toString())
            .externalKindId("kind5");
        ProductKind termDeposit = new ProductKind().kindName(BackbaseProductKind.TERM_DEPOSIT.toString())
            .externalKindId("kind6");
        ProductKind loan = new ProductKind().kindName(BackbaseProductKind.LOAN.toString()).externalKindId("kind7");

        ProductKind insurance = new ProductKind().kindName("Insurance").externalKindId("insurance")
            .kindUri("insurance");

        ProductCatalog productCatalog = new ProductCatalog();
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

        log.info("Setup Product Catalog Request: ");

        Mono<ProductCatalog> productCatalogMono = productCatalogService.setupProductCatalog(productCatalog);
        StepVerifier.create(productCatalogMono)
            .expectNextCount(1)
            .verifyComplete();

    }

    private ProductType createProductType(String id, String name, ProductKind productKind) {
        ProductType productType = new ProductType()
            .externalProductId(id)
            .externalProductKindId(productKind.getExternalKindId())
//                .productKind(productKind)
            .externalProductTypeId(productKind.getKindName())
            .productTypeName(name);
        return productType;
    }

    protected com.backbase.dbs.accounts.presentation.service.ApiClient getAccountsPresentationClient(
        WebClient webClient) {
        return new com.backbase.dbs.accounts.presentation.service.ApiClient(webClient, getObjectMapper(),
            getDateFormat())
            .setBasePath("https://stream-api.proto.backbasecloud.com/account-presentation-service/service-api/v2");
    }
}
