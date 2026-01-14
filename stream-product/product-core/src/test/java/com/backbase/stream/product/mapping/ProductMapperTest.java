package com.backbase.stream.product.mapping;

import com.backbase.dbs.arrangement.api.integration.v3.model.ArrangementPost;
import com.backbase.dbs.arrangement.api.integration.v3.model.ArrangementStateIdentification;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementItem;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementPutItem;
import com.backbase.stream.legalentity.model.AvailableBalance;
import com.backbase.stream.legalentity.model.BaseProduct;
import com.backbase.stream.legalentity.model.BaseProductState;
import com.backbase.stream.legalentity.model.BookedBalance;
import com.backbase.stream.legalentity.model.CreditCard;
import com.backbase.stream.legalentity.model.CreditLimit;
import com.backbase.stream.legalentity.model.CurrentAccount;
import com.backbase.stream.legalentity.model.CurrentInvestment;
import com.backbase.stream.legalentity.model.DebitCard;
import com.backbase.stream.legalentity.model.DebitCardItem;
import com.backbase.stream.legalentity.model.InterestDetails;
import com.backbase.stream.legalentity.model.InterestPaymentFrequencyUnit;
import com.backbase.stream.legalentity.model.InvestmentAccount;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityReference;
import com.backbase.stream.legalentity.model.Loan;
import com.backbase.stream.legalentity.model.PrincipalAmount;
import com.backbase.stream.legalentity.model.Product;
import com.backbase.stream.legalentity.model.ReservedAmount;
import com.backbase.stream.legalentity.model.SavingsAccount;
import com.backbase.stream.legalentity.model.TermDeposit;
import com.backbase.stream.legalentity.model.TermUnit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ProductMapperTest {

    private final ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);

    LegalEntityReference buildLegalEntityReference(String legalEntityExternalId) {
        return new LegalEntityReference(null, legalEntityExternalId);
    }

    private void buildBaseProduct(BaseProduct baseProduct) {
        baseProduct
            .externalId("prod_ext_id")
            .productTypeExternalId("prod_type_ext_id")
            .legalEntities(List.of(buildLegalEntityReference("le_ext_id_1"), buildLegalEntityReference("le_ext_id_2")))
            // Here externalStateId and state are set because in arrangement-pull-integration-service
            // in the AccountStatusMapper, both fields are set with the same state value in the 3
            // ACTIVE, INACTIVE and CLOSED enums
            .state(new BaseProductState().externalStateId("prod_state").state("prod_state"))
            .additions(Map.of("add_prop_1", "add_val_1", "add_prop_2", "add_val_2"))
            .name("prod_name")
            .currency("USD")
            .externalTransferAllowed(true)
            .accountOpeningDate(OffsetDateTime.parse("2022-05-23T11:20:30.000001Z"))
            .amountInArrear(BigDecimal.TEN)
            .lastUpdateDate(OffsetDateTime.parse("2022-10-01T11:20:30.000001Z"))
            .bankAlias("bank_alias")
            .sourceId("source_id")
            .externalParentId("ext_parent_id")
            .interestDetails((InterestDetails) new InterestDetails().annualPercentageYield(new BigDecimal("12.5")))
            .overdueSince(LocalDate.of(2022, 11, 2))
            .debitCardsItems(List.of(new DebitCardItem().cardId("card_1"), new DebitCardItem().cardId("card_2")));
    }

    private Product buildProduct() {
        Product product = new Product();
        buildBaseProduct(product);
        return product
            .accountHolderName("John Doe")
            .bookedBalance(new BookedBalance().amount(new BigDecimal("2500")))
            .availableBalance(new AvailableBalance().amount(new BigDecimal("3000")))
            .reservedAmount(new ReservedAmount().amount(new BigDecimal("3000")))
            .creditLimit(new CreditLimit().amount(new BigDecimal("10000")))
            .IBAN("iban")
            .BBAN("bban")
            .BIC("bic")
            .urgentTransferAllowed(false)
            .accruedInterest(new BigDecimal("3.4"))
            .number("1234567890")
            .principalAmount(new PrincipalAmount().amount(new BigDecimal("15000")))
            .productNumber("123")
            .bankBranchCode("branch_code")
            .accountInterestRate(new BigDecimal("4.5"))
            .valueDateBalance(new BigDecimal("1.5"))
            .creditLimitUsage(new BigDecimal("100"))
            .creditLimitInterestRate(new BigDecimal("7"))
            .creditLimitExpiryDate(OffsetDateTime.parse("2022-12-23T11:20:30.000001Z"))
            .startDate(OffsetDateTime.parse("2021-05-23T11:20:30.000001Z"))
            .termUnit(TermUnit.MONTHLY)
            .termNumber(new BigDecimal("6"))
            .interestPaymentFrequencyNumber(new BigDecimal("2"))
            .interestPaymentFrequencyUnit(InterestPaymentFrequencyUnit.DAILY)
            .maturityDate(OffsetDateTime.parse("2023-12-23T11:20:30.000001Z"))
            .maturityAmount(new BigDecimal("1200"))
            .autoRenewalIndicator(true)
            .interestSettlementAccount("ISA")
            .outstandingPrincipalAmount(new BigDecimal("101"))
            .monthlyInstalmentAmount(new BigDecimal("250"))
            .validThru(OffsetDateTime.parse("2050-12-23T11:20:30.000001Z"));
    }

    private SavingsAccount buildSavingsAccount() {
        SavingsAccount savingsAccount = new SavingsAccount();
        buildBaseProduct(savingsAccount);
        return savingsAccount;
    }

    private DebitCard buildDebitCard() {
        DebitCard debitCard = new DebitCard();
        buildBaseProduct(debitCard);
        return debitCard;
    }

    private CreditCard buildCreditCard() {
        CreditCard creditCard = new CreditCard();
        buildBaseProduct(creditCard);
        return creditCard;
    }

    private TermDeposit buildTermDeposit() {
        TermDeposit termDeposit = new TermDeposit();
        buildBaseProduct(termDeposit);
        return termDeposit;
    }

    private InvestmentAccount buildInvestmentAccount() {
        InvestmentAccount investmentAccount = new InvestmentAccount();
        buildBaseProduct(investmentAccount);
        return investmentAccount;
    }

    private Loan buildLoan() {
        Loan loan = new Loan();
        buildBaseProduct(loan);
        return loan;
    }

    @Test
    void map_Product_To_AccountArrangementItemPost() {
        Product source = buildProduct();
        ArrangementPost target = productMapper.toPresentation(source);
        Assertions.assertEquals(source.getExternalId(), target.getExternalId());
        Assertions.assertEquals(source.getProductTypeExternalId(), target.getProduct().getExternalId());
        Assertions.assertNotNull(target.getLegalEntities());
        Assertions.assertEquals(source.getLegalEntities().size(), target.getLegalEntities().size());
        Assertions.assertEquals(source.getState().getExternalStateId(), target.getState().getExternalId());
        Assertions.assertEquals(source.getAccountHolderName(), target.getAccountHolder().getNames());
    }

    @Test
    void map_BaseProduct_To_AccountArrangementItemPost() {
        Product source = buildProduct();
        ArrangementPost target = productMapper.toPresentation(source);
        Assertions.assertEquals(source.getExternalId(), target.getExternalId());
        Assertions.assertEquals(source.getProductTypeExternalId(), target.getProduct().getExternalId());
        Assertions.assertNotNull(target.getLegalEntities());
        Assertions.assertEquals(source.getLegalEntities().size(), target.getLegalEntities().size());
        Assertions.assertEquals(source.getState().getExternalStateId(), target.getState().getExternalId());
        Assertions.assertEquals(source.getAccountHolderName(), target.getAccountHolder().getNames());
        Assertions.assertEquals(source.getExternalParentId(), target.getParentExternalId());
    }

    @Test
    void map_SavingsAccount_To_AccountArrangementItemPost() {
        SavingsAccount source = buildSavingsAccount();
        ArrangementPost target = productMapper.toPresentation(source);
        Assertions.assertEquals(source.getExternalId(), target.getExternalId());
        Assertions.assertEquals(source.getProductTypeExternalId(), target.getProduct().getExternalId());
        Assertions.assertNotNull(target.getLegalEntities());
        Assertions.assertEquals(source.getLegalEntities().size(), target.getLegalEntities().size());
        Assertions.assertNotNull(target.getDebitCards());
        Assertions.assertEquals(source.getDebitCardsItems().size(), target.getDebitCards().size());
        Assertions.assertEquals(source.getState().getExternalStateId(), target.getState().getExternalId());
        Assertions.assertEquals(source.getAccountHolderName(), target.getAccountHolder().getNames());
        Assertions.assertEquals(source.getExternalParentId(), target.getParentExternalId());
    }

    @Test
    void map_DebitCard_To_AccountArrangementItemPost() {
        DebitCard source = buildDebitCard();
        ArrangementPost target = productMapper.toPresentation(source);
        Assertions.assertEquals(source.getExternalId(), target.getExternalId());
        Assertions.assertEquals(source.getProductTypeExternalId(), target.getProduct().getExternalId());
        Assertions.assertNotNull(target.getLegalEntities());
        Assertions.assertEquals(source.getLegalEntities().size(), target.getLegalEntities().size());
        Assertions.assertEquals(source.getState().getExternalStateId(), target.getState().getExternalId());
        Assertions.assertEquals(source.getAccountHolderName(), target.getAccountHolder().getNames());
        Assertions.assertEquals(source.getExternalParentId(), target.getParentExternalId());
    }

    @Test
    void map_CreditCard_To_AccountArrangementItemPost() {
        CreditCard source = buildCreditCard();
        ArrangementPost target = productMapper.toPresentation(source);
        Assertions.assertEquals(source.getExternalId(), target.getExternalId());
        Assertions.assertEquals(source.getProductTypeExternalId(), target.getProduct().getExternalId());
        Assertions.assertNotNull(target.getLegalEntities());
        Assertions.assertEquals(source.getLegalEntities().size(), target.getLegalEntities().size());
        Assertions.assertEquals(source.getState().getExternalStateId(), target.getState().getExternalId());
        Assertions.assertEquals(source.getAccountHolderName(), target.getAccountHolder().getNames());
        Assertions.assertEquals(source.getExternalParentId(), target.getParentExternalId());
    }

    @Test
    void map_TermDeposit_To_AccountArrangementItemPost() {
        TermDeposit source = buildTermDeposit();
        ArrangementPost target = productMapper.toPresentation(source);
        Assertions.assertEquals(source.getExternalId(), target.getExternalId());
        Assertions.assertEquals(source.getProductTypeExternalId(), target.getProduct().getExternalId());
        Assertions.assertNotNull(target.getLegalEntities());
        Assertions.assertEquals(source.getLegalEntities().size(), target.getLegalEntities().size());
        Assertions.assertEquals(source.getState().getExternalStateId(), target.getState().getExternalId());
        Assertions.assertEquals(source.getAccountHolderName(), target.getAccountHolder().getNames());
        Assertions.assertEquals(source.getExternalParentId(), target.getParentExternalId());
    }

    @Test
    void map_InvestmentAccount_To_AccountArrangementItemPost() {
        InvestmentAccount source = buildInvestmentAccount();
        ArrangementPost target = productMapper.toPresentation(source);
        Assertions.assertEquals(source.getExternalId(), target.getExternalId());
        Assertions.assertEquals(source.getProductTypeExternalId(), target.getProduct().getExternalId());
        Assertions.assertNotNull(target.getLegalEntities());
        Assertions.assertEquals(source.getLegalEntities().size(), target.getLegalEntities().size());
        Assertions.assertEquals(source.getState().getExternalStateId(), target.getState().getExternalId());
        Assertions.assertEquals(source.getExternalParentId(), target.getParentExternalId());
    }

    @Test
    void map_Loan_To_AccountArrangementItemPost() {
        Loan source = buildLoan();
        ArrangementPost target = productMapper.toPresentation(source);
        Assertions.assertEquals(source.getExternalId(), target.getExternalId());
        Assertions.assertEquals(source.getProductTypeExternalId(), target.getProduct().getExternalId());
        Assertions.assertNotNull(target.getLegalEntities());
        Assertions.assertEquals(source.getLegalEntities().size(), target.getLegalEntities().size());
        Assertions.assertEquals(source.getState().getExternalStateId(), target.getState().getExternalId());
        Assertions.assertEquals(source.getAccountHolderName(), target.getAccountHolder().getNames());
        Assertions.assertEquals(source.getExternalParentId(), target.getParentExternalId());
    }

    @Test
    void map_AccountArrangementItemPost_To_AccountArrangementItem() {
        ArrangementPost source = productMapper.toPresentation(buildProduct());
        source.setState(new ArrangementStateIdentification().externalId("123"));
        ArrangementPutItem target = productMapper.toArrangementItemPut(source);

        Assertions.assertEquals(target.getExternalArrangementId(), source.getExternalId());
        Assertions.assertEquals(String.valueOf(target.getStateId()), source.getState().getExternalId());
        Assertions.assertEquals(target.getAccountHolderNames(), source.getAccountHolder().getNames());
    }

    @Test
    void map_AccountArrangementItemPost_To_AccountArrangementItemPut() {
        ArrangementPost source = productMapper.toPresentation(buildProduct());
        source.setState(new ArrangementStateIdentification().externalId("123"));
        ArrangementPutItem target = productMapper.toArrangementItemPut(source);

        Assertions.assertEquals(target.getExternalArrangementId(), source.getExternalId());
        Assertions.assertEquals(String.valueOf(target.getStateId()), source.getState().getExternalId());
        Assertions.assertEquals(target.getAccountHolderNames(), source.getAccountHolder().getNames());
    }

    @Test
    void map_AccountArrangementItemBase_To_AccountArrangementItem() {
        ArrangementPost source = productMapper.toPresentation(buildProduct());
        ArrangementItem target = productMapper.toArrangementItem(source);
        Assertions.assertEquals(target.getExternalArrangementId(), source.getExternalId());
        Assertions.assertNotNull(target.getState());
        Assertions.assertEquals(target.getState().getState(), source.getState().getExternalId());
        Assertions.assertEquals(target.getAccountHolderNames(), source.getAccountHolder().getNames());
    }

    @Test
    void map_Product_To_AccountArrangementItem() {
        Product source = buildProduct();
        ArrangementItem target = productMapper.toPresentationWithWeirdSpellingError(source);
        Assertions.assertEquals(source.getExternalId(), target.getExternalArrangementId());
        Assertions.assertEquals(source.getState().getExternalStateId(), target.getExternalStateId());
    }

    @Test
    void map_AccountArrangementItem_To_Product() {
        ArrangementItem source = productMapper.toArrangementItem(productMapper.toPresentation(buildProduct()));
        Product target = productMapper.mapCustomProduct(source);
        Assertions.assertEquals(target.getExternalId(), source.getExternalArrangementId());
        Assertions.assertEquals(target.getProductTypeExternalId(), source.getExternalProductId());
    }

    @Test
    void map_AccountArrangementItem_To_CurrentAccount() {
        ArrangementItem source = productMapper.toArrangementItem(productMapper.toPresentation(buildProduct()));
        CurrentAccount target = productMapper.mapCurrentAccount(source);
        Assertions.assertEquals(target.getExternalId(), source.getExternalArrangementId());
        Assertions.assertEquals(target.getProductTypeExternalId(), source.getExternalProductId());
    }

    @Test
    void map_AccountArrangementItem_To_SavingsAccount() {
        ArrangementItem source = productMapper.toArrangementItem(productMapper.toPresentation(buildProduct()));
        SavingsAccount target = productMapper.mapSavingAccount(source);
        Assertions.assertEquals(target.getExternalId(), source.getExternalArrangementId());
        Assertions.assertEquals(target.getProductTypeExternalId(), source.getExternalProductId());
    }

    @Test
    void map_AccountArrangementItem_To_DebitCard() {
        ArrangementItem source = productMapper.toArrangementItem(productMapper.toPresentation(buildProduct()));
        DebitCard target = productMapper.mapDebitCard(source);
        Assertions.assertEquals(target.getExternalId(), source.getExternalArrangementId());
        Assertions.assertEquals(target.getProductTypeExternalId(), source.getExternalProductId());
    }

    @Test
    void map_AccountArrangementItem_To_CreditCard() {
        ArrangementItem source = productMapper.toArrangementItem(productMapper.toPresentation(buildProduct()));
        CreditCard target = productMapper.mapCreditCard(source);
        Assertions.assertEquals(target.getExternalId(), source.getExternalArrangementId());
        Assertions.assertEquals(target.getProductTypeExternalId(), source.getExternalProductId());
    }

    @Test
    void map_AccountArrangementItem_To_Loan() {
        ArrangementItem source = productMapper.toArrangementItem(productMapper.toPresentation(buildProduct()));
        Loan target = productMapper.mapLoan(source);
        Assertions.assertEquals(target.getExternalId(), source.getExternalArrangementId());
        Assertions.assertEquals(target.getProductTypeExternalId(), source.getExternalProductId());
    }

    @Test
    void map_AccountArrangementItem_To_TermDeposit() {
        ArrangementItem source = productMapper.toArrangementItem(productMapper.toPresentation(buildProduct()));
        TermDeposit target = productMapper.mapTermDeposit(source);
        Assertions.assertEquals(target.getExternalId(), source.getExternalArrangementId());
        Assertions.assertEquals(target.getProductTypeExternalId(), source.getExternalProductId());
    }

    @Test
    void map_AccountArrangementItem_To_InvestmentAccount() {
        ArrangementItem source = productMapper.toArrangementItem(productMapper.toPresentation(buildProduct()));
        InvestmentAccount target = productMapper.mapInvestmentAccount(source);
        Assertions.assertEquals(target.getExternalId(), source.getExternalArrangementId());
        Assertions.assertEquals(target.getProductTypeExternalId(), source.getExternalProductId());
    }

    @Test
    void mapBookedBalance() {
        BigDecimal source = new BigDecimal("130.79");
        BookedBalance target = productMapper.mapBookedBalance(source);
        Assertions.assertEquals(source, target.getAmount());
    }

    @Test
    public void mapReservedAmount() {
        BigDecimal source = new BigDecimal("130.79");
        ReservedAmount target = productMapper.mapReservedAmount(source);
        Assertions.assertEquals(source, target.getAmount());
    }

    @Test
    void mapAvailable() {
        BigDecimal source = new BigDecimal("130.79");
        AvailableBalance target = productMapper.mapAvailable(source);
        Assertions.assertEquals(source, target.getAmount());
    }

    @Test
    void mapCurrentInvestment() {
        BigDecimal source = new BigDecimal("12345.69");
        CurrentInvestment target = productMapper.mapCurrentInvestment(source);
        Assertions.assertEquals(source, target.getAmount());
    }

    @Test
    void mapPrincipal() {
        BigDecimal source = new BigDecimal("130.79");
        PrincipalAmount target = productMapper.mapPrincipal(source);
        Assertions.assertEquals(source, target.getAmount());
    }

    @Test
    void mapCreditLimit() {
        BigDecimal source = new BigDecimal("130.79");
        CreditLimit target = productMapper.mapCreditLimit(source);
        Assertions.assertEquals(source, target.getAmount());
    }

    @Test
    void mapLegalEntity() {
        LegalEntity target = productMapper.mapLegalEntity("leid");
        Assertions.assertEquals("leid", target.getExternalId());
    }

    @Test
    void map_TermUnit_To_TimeUnit() {
        Assertions.assertNull(productMapper.map(TermUnit.QUARTERLY));
        Assertions.assertEquals(com.backbase.dbs.arrangement.api.integration.v3.model.TimeUnit.D, productMapper.map(TermUnit.DAILY));
        Assertions.assertEquals(com.backbase.dbs.arrangement.api.integration.v3.model.TimeUnit.W, productMapper.map(TermUnit.WEEKLY));
        Assertions.assertEquals(com.backbase.dbs.arrangement.api.integration.v3.model.TimeUnit.M, productMapper.map(TermUnit.MONTHLY));
        Assertions.assertEquals(com.backbase.dbs.arrangement.api.integration.v3.model.TimeUnit.Y, productMapper.map(TermUnit.YEARLY));
    }

    @Test
    void map_InterestPaymentFrequencyUnit_To_TimeUnit() {
        Assertions.assertNull(productMapper.map(InterestPaymentFrequencyUnit.QUARTERLY));
        Assertions.assertEquals(com.backbase.dbs.arrangement.api.integration.v3.model.TimeUnit.D, productMapper.map(InterestPaymentFrequencyUnit.DAILY));
        Assertions.assertEquals(com.backbase.dbs.arrangement.api.integration.v3.model.TimeUnit.W, productMapper.map(InterestPaymentFrequencyUnit.WEEKLY));
        Assertions.assertEquals(com.backbase.dbs.arrangement.api.integration.v3.model.TimeUnit.M, productMapper.map(InterestPaymentFrequencyUnit.MONTHLY));
        Assertions.assertEquals(com.backbase.dbs.arrangement.api.integration.v3.model.TimeUnit.Y, productMapper.map(InterestPaymentFrequencyUnit.YEARLY));
    }

    @Test
    void map_TimeUnit_TermUnit() {
        Assertions.assertEquals(TermUnit.DAILY, productMapper.map(com.backbase.dbs.arrangement.api.integration.v3.model.TimeUnit.D));
        Assertions.assertEquals(TermUnit.WEEKLY, productMapper.map(com.backbase.dbs.arrangement.api.integration.v3.model.TimeUnit.W));
        Assertions.assertEquals(TermUnit.MONTHLY, productMapper.map(com.backbase.dbs.arrangement.api.integration.v3.model.TimeUnit.M));
        Assertions.assertEquals(TermUnit.YEARLY, productMapper.map(com.backbase.dbs.arrangement.api.integration.v3.model.TimeUnit.Y));
    }

    @Test
    void map_TimeUnit_To_InterestPaymentFrequencyUnit() {
        Assertions.assertNull(productMapper.mapInterestPayment(null));
        Assertions.assertEquals(InterestPaymentFrequencyUnit.DAILY, productMapper.mapInterestPayment(com.backbase.dbs.arrangement.api.integration.v3.model.TimeUnit.D));
        Assertions.assertEquals(InterestPaymentFrequencyUnit.WEEKLY, productMapper.mapInterestPayment(com.backbase.dbs.arrangement.api.integration.v3.model.TimeUnit.W));
        Assertions.assertEquals(InterestPaymentFrequencyUnit.MONTHLY, productMapper.mapInterestPayment(com.backbase.dbs.arrangement.api.integration.v3.model.TimeUnit.M));
        Assertions.assertEquals(InterestPaymentFrequencyUnit.YEARLY, productMapper.mapInterestPayment(com.backbase.dbs.arrangement.api.integration.v3.model.TimeUnit.Y));
    }

    @Test
    void mapLegalEntityId() {
        Assertions.assertNull(productMapper.mapLegalEntityId(null));

        List<LegalEntityReference> legalEntityReferenceList = List.of(buildLegalEntityReference("leid_1"), buildLegalEntityReference("leid_2"));
        List<String> legalEntityIdList = productMapper.mapLegalEntityId(legalEntityReferenceList);
        Assertions.assertEquals(legalEntityReferenceList.size(), legalEntityIdList.size());

        Assertions.assertNull(productMapper.mapLegalEntityReference(null));
        Assertions.assertEquals(legalEntityIdList.size(), productMapper.mapLegalEntityReference(legalEntityIdList).size());
    }
}