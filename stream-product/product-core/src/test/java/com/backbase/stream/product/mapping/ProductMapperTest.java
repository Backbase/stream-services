package com.backbase.stream.product.mapping;

import com.backbase.dbs.arrangement.api.service.v2.model.*;
import com.backbase.stream.legalentity.model.*;
import com.backbase.stream.legalentity.model.DebitCardItem;
import com.backbase.stream.legalentity.model.InterestDetails;
import com.backbase.stream.legalentity.model.UserPreferences;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class ProductMapperTest {

    private final ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);

    LegalEntityReference buildLegalEntityReference(String legalEntityExternalId) {
        return new LegalEntityReference().externalId(legalEntityExternalId);
    }

    private void buildBaseProduct(BaseProduct baseProduct) {
        baseProduct
                .externalId("prod_ext_id")
                .productTypeExternalId("prod_type_ext_id")
                .legalEntities(List.of(buildLegalEntityReference("le_ext_id_1"), buildLegalEntityReference("le_ext_id_2")))
                .state(new BaseProductState().state("prod_state"))
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
                .monthlyInstalmentAmount(new BigDecimal("250"));
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
    public void map_Product_To_AccountArrangementItemPost() {
        Product source = buildProduct();
        AccountArrangementItemPost target = productMapper.toPresentation(source);
        Assertions.assertEquals(source.getExternalId(), target.getExternalArrangementId());
        Assertions.assertEquals(source.getProductTypeExternalId(), target.getExternalProductId());
        Assertions.assertNotNull(target.getExternalLegalEntityIds());
        Assertions.assertEquals(source.getLegalEntities().size(), target.getExternalLegalEntityIds().size());
        Assertions.assertEquals(source.getState().getExternalStateId(), target.getExternalStateId());
        Assertions.assertEquals(source.getAccountHolderName(), target.getAccountHolderNames());
    }

    @Test
    public void map_BaseProduct_To_AccountArrangementItemPost() {
        Product source = buildProduct();
        AccountArrangementItemPost target = productMapper.toPresentation(source);
        Assertions.assertEquals(source.getExternalId(), target.getExternalArrangementId());
        Assertions.assertEquals(source.getProductTypeExternalId(), target.getExternalProductId());
        Assertions.assertNotNull(target.getExternalLegalEntityIds());
        Assertions.assertEquals(source.getLegalEntities().size(), target.getExternalLegalEntityIds().size());
        Assertions.assertEquals(source.getState().getExternalStateId(), target.getExternalStateId());
        Assertions.assertEquals(source.getAccountHolderName(), target.getAccountHolderNames());
    }

    @Test
    public void map_SavingsAccount_To_AccountArrangementItemPost() {
        SavingsAccount source = buildSavingsAccount();
        AccountArrangementItemPost target = productMapper.toPresentation(source);
        Assertions.assertEquals(source.getExternalId(), target.getExternalArrangementId());
        Assertions.assertEquals(source.getProductTypeExternalId(), target.getExternalProductId());
        Assertions.assertNotNull(target.getExternalLegalEntityIds());
        Assertions.assertEquals(source.getLegalEntities().size(), target.getExternalLegalEntityIds().size());
        Assertions.assertNotNull(target.getDebitCards());
        Assertions.assertEquals(source.getDebitCardsItems().size(), target.getDebitCards().size());
        Assertions.assertEquals(source.getState().getExternalStateId(), target.getExternalStateId());
        Assertions.assertEquals(source.getAccountHolderName(), target.getAccountHolderNames());
    }

    @Test
    public void map_DebitCard_To_AccountArrangementItemPost() {
        DebitCard source = buildDebitCard();
        AccountArrangementItemPost target = productMapper.toPresentation(source);
        Assertions.assertEquals(source.getExternalId(), target.getExternalArrangementId());
        Assertions.assertEquals(source.getProductTypeExternalId(), target.getExternalProductId());
        Assertions.assertNotNull(target.getExternalLegalEntityIds());
        Assertions.assertEquals(source.getLegalEntities().size(), target.getExternalLegalEntityIds().size());
        Assertions.assertEquals(source.getState().getExternalStateId(), target.getExternalStateId());
        Assertions.assertEquals(source.getAccountHolderName(), target.getAccountHolderNames());
    }

    @Test
    public void map_CreditCard_To_AccountArrangementItemPost() {
        CreditCard source = buildCreditCard();
        AccountArrangementItemPost target = productMapper.toPresentation(source);
        Assertions.assertEquals(source.getExternalId(), target.getExternalArrangementId());
        Assertions.assertEquals(source.getProductTypeExternalId(), target.getExternalProductId());
        Assertions.assertNotNull(target.getExternalLegalEntityIds());
        Assertions.assertEquals(source.getLegalEntities().size(), target.getExternalLegalEntityIds().size());
        Assertions.assertEquals(source.getState().getExternalStateId(), target.getExternalStateId());
        Assertions.assertEquals(source.getAccountHolderName(), target.getAccountHolderNames());
    }

    @Test
    public void map_TermDeposit_To_AccountArrangementItemPost() {
        TermDeposit source = buildTermDeposit();
        AccountArrangementItemPost target = productMapper.toPresentation(source);
        Assertions.assertEquals(source.getExternalId(), target.getExternalArrangementId());
        Assertions.assertEquals(source.getProductTypeExternalId(), target.getExternalProductId());
        Assertions.assertNotNull(target.getExternalLegalEntityIds());
        Assertions.assertEquals(source.getLegalEntities().size(), target.getExternalLegalEntityIds().size());
        Assertions.assertEquals(source.getState().getExternalStateId(), target.getExternalStateId());
        Assertions.assertEquals(source.getAccountHolderName(), target.getAccountHolderNames());
    }

    @Test
    public void map_InvestmentAccount_To_AccountArrangementItemPost() {
        InvestmentAccount source = buildInvestmentAccount();
        AccountArrangementItemPost target = productMapper.toPresentation(source);
        Assertions.assertEquals(source.getExternalId(), target.getExternalArrangementId());
        Assertions.assertEquals(source.getProductTypeExternalId(), target.getExternalProductId());
        Assertions.assertNotNull(target.getExternalLegalEntityIds());
        Assertions.assertEquals(source.getLegalEntities().size(), target.getExternalLegalEntityIds().size());
        Assertions.assertEquals(source.getState().getExternalStateId(), target.getExternalStateId());
    }

    @Test
    public void map_Loan_To_AccountArrangementItemPost() {
        Loan source = buildLoan();
        AccountArrangementItemPost target = productMapper.toPresentation(source);
        Assertions.assertEquals(source.getExternalId(), target.getExternalArrangementId());
        Assertions.assertEquals(source.getProductTypeExternalId(), target.getExternalProductId());
        Assertions.assertNotNull(target.getExternalLegalEntityIds());
        Assertions.assertEquals(source.getLegalEntities().size(), target.getExternalLegalEntityIds().size());
        Assertions.assertEquals(source.getState().getExternalStateId(), target.getExternalStateId());
        Assertions.assertEquals(source.getAccountHolderName(), target.getAccountHolderNames());
    }

    @Test
    public void map_AccountArrangementItemPost_To_AccountArrangementItem() {
        AccountArrangementItemPost source = productMapper.toPresentation(buildProduct());
        AccountArrangementItem target = productMapper.toArrangementItem(source);
        Assertions.assertEquals(target.getExternalArrangementId(), source.getExternalArrangementId());
        Assertions.assertEquals(target.getExternalProductId(), source.getExternalProductId());
        Assertions.assertEquals(target.getExternalStateId(), source.getExternalStateId());
        Assertions.assertEquals(target.getAccountHolderNames(), source.getAccountHolderNames());
    }

    @Test
    public void map_AccountArrangementItemPost_To_AccountArrangementItemPut() {
        AccountArrangementItemPost source = productMapper.toPresentation(buildProduct());
        AccountArrangementItemPut target = productMapper.toArrangementItemPut(source);
        Assertions.assertEquals(target.getExternalArrangementId(), source.getExternalArrangementId());
        Assertions.assertEquals(target.getExternalStateId(), source.getExternalStateId());
        Assertions.assertEquals(target.getAccountHolderNames(), source.getAccountHolderNames());
    }

    @Test
    public void map_AccountArrangementItemBase_To_AccountArrangementItem() {
        AccountArrangementItemPost source = productMapper.toPresentation(buildProduct());
        AccountArrangementItem target = productMapper.toArrangementItem(source);
        Assertions.assertEquals(target.getExternalArrangementId(), source.getExternalArrangementId());
        Assertions.assertEquals(target.getExternalStateId(), source.getExternalStateId());
        Assertions.assertEquals(target.getAccountHolderNames(), source.getAccountHolderNames());
    }

    @Test
    public void map_Product_To_AccountArrangementItem() {
        Product source = buildProduct();
        AccountArrangementItem target = productMapper.toPresentationWithWeirdSpellingError(source);
        Assertions.assertEquals(target.getExternalArrangementId(), source.getExternalId());
        Assertions.assertEquals(target.getExternalStateId(), source.getState().getExternalStateId());
    }

    @Test
    public void map_AccountArrangementItem_To_Product() {
        AccountArrangementItem source = productMapper.toArrangementItem(productMapper.toPresentation(buildProduct()));
        Product target = productMapper.mapCustomProduct(source);
        Assertions.assertEquals(target.getExternalId(), source.getExternalArrangementId());
        Assertions.assertEquals(target.getProductTypeExternalId(), source.getExternalProductId());
    }

    @Test
    public void map_AccountArrangementItem_To_CurrentAccount() {
        AccountArrangementItem source = productMapper.toArrangementItem(productMapper.toPresentation(buildProduct()));
        CurrentAccount target = productMapper.mapCurrentAccount(source);
        Assertions.assertEquals(target.getExternalId(), source.getExternalArrangementId());
        Assertions.assertEquals(target.getProductTypeExternalId(), source.getExternalProductId());
    }

    @Test
    public void map_AccountArrangementItem_To_SavingsAccount() {
        AccountArrangementItem source = productMapper.toArrangementItem(productMapper.toPresentation(buildProduct()));
        SavingsAccount target = productMapper.mapSavingAccount(source);
        Assertions.assertEquals(target.getExternalId(), source.getExternalArrangementId());
        Assertions.assertEquals(target.getProductTypeExternalId(), source.getExternalProductId());
    }

    @Test
    public void map_AccountArrangementItem_To_DebitCard() {
        AccountArrangementItem source = productMapper.toArrangementItem(productMapper.toPresentation(buildProduct()));
        DebitCard target = productMapper.mapDebitCard(source);
        Assertions.assertEquals(target.getExternalId(), source.getExternalArrangementId());
        Assertions.assertEquals(target.getProductTypeExternalId(), source.getExternalProductId());
    }

    @Test
    public void map_AccountArrangementItem_To_CreditCard() {
        AccountArrangementItem source = productMapper.toArrangementItem(productMapper.toPresentation(buildProduct()));
        CreditCard target = productMapper.mapCreditCard(source);
        Assertions.assertEquals(target.getExternalId(), source.getExternalArrangementId());
        Assertions.assertEquals(target.getProductTypeExternalId(), source.getExternalProductId());
    }

    @Test
    public void map_AccountArrangementItem_To_Loan() {
        AccountArrangementItem source = productMapper.toArrangementItem(productMapper.toPresentation(buildProduct()));
        Loan target = productMapper.mapLoan(source);
        Assertions.assertEquals(target.getExternalId(), source.getExternalArrangementId());
        Assertions.assertEquals(target.getProductTypeExternalId(), source.getExternalProductId());
    }

    @Test
    public void map_AccountArrangementItem_To_TermDeposit() {
        AccountArrangementItem source = productMapper.toArrangementItem(productMapper.toPresentation(buildProduct()));
        TermDeposit target = productMapper.mapTermDeposit(source);
        Assertions.assertEquals(target.getExternalId(), source.getExternalArrangementId());
        Assertions.assertEquals(target.getProductTypeExternalId(), source.getExternalProductId());
    }

    @Test
    public void map_AccountArrangementItem_To_InvestmentAccount() {
        AccountArrangementItem source = productMapper.toArrangementItem(productMapper.toPresentation(buildProduct()));
        InvestmentAccount target = productMapper.mapInvestmentAccount(source);
        Assertions.assertEquals(target.getExternalId(), source.getExternalArrangementId());
        Assertions.assertEquals(target.getProductTypeExternalId(), source.getExternalProductId());
    }

    @Test
    public void mapBookedBalance() {
        BigDecimal source = new BigDecimal("130.79");
        BookedBalance target = productMapper.mapBookedBalance(source);
        Assertions.assertEquals(source, target.getAmount());
    }

    @Test
    public void mapAvailable() {
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
    public void mapPrincipal() {
        BigDecimal source = new BigDecimal("130.79");
        PrincipalAmount target = productMapper.mapPrincipal(source);
        Assertions.assertEquals(source, target.getAmount());
    }

    @Test
    public void mapCreditLimit() {
        BigDecimal source = new BigDecimal("130.79");
        CreditLimit target = productMapper.mapCreditLimit(source);
        Assertions.assertEquals(source, target.getAmount());
    }

    @Test
    public void mapLegalEntity() {
        LegalEntity target = productMapper.mapLegalEntity("leid");
        Assertions.assertEquals("leid", target.getExternalId());
    }

    @Test
    public void map_TermUnit_To_TimeUnit() {
        Assertions.assertNull(productMapper.map(TermUnit.QUARTERLY));
        Assertions.assertEquals(TimeUnit.D, productMapper.map(TermUnit.DAILY));
        Assertions.assertEquals(TimeUnit.W, productMapper.map(TermUnit.WEEKLY));
        Assertions.assertEquals(TimeUnit.M, productMapper.map(TermUnit.MONTHLY));
        Assertions.assertEquals(TimeUnit.Y, productMapper.map(TermUnit.YEARLY));
    }

    @Test
    public void map_InterestPaymentFrequencyUnit_To_TimeUnit() {
        Assertions.assertNull(productMapper.map(InterestPaymentFrequencyUnit.QUARTERLY));
        Assertions.assertEquals(TimeUnit.D, productMapper.map(InterestPaymentFrequencyUnit.DAILY));
        Assertions.assertEquals(TimeUnit.W, productMapper.map(InterestPaymentFrequencyUnit.WEEKLY));
        Assertions.assertEquals(TimeUnit.M, productMapper.map(InterestPaymentFrequencyUnit.MONTHLY));
        Assertions.assertEquals(TimeUnit.Y, productMapper.map(InterestPaymentFrequencyUnit.YEARLY));
    }

    @Test
    public void map_TimeUnit_TermUnit() {
        Assertions.assertEquals(TermUnit.DAILY, productMapper.map(TimeUnit.D));
        Assertions.assertEquals(TermUnit.WEEKLY, productMapper.map(TimeUnit.W));
        Assertions.assertEquals(TermUnit.MONTHLY, productMapper.map(TimeUnit.M));
        Assertions.assertEquals(TermUnit.YEARLY, productMapper.map(TimeUnit.Y));
    }

    @Test
    public void map_TimeUnit_To_InterestPaymentFrequencyUnit() {
        Assertions.assertNull(productMapper.mapInterestPayment(null));
        Assertions.assertEquals(InterestPaymentFrequencyUnit.DAILY, productMapper.mapInterestPayment(TimeUnit.D));
        Assertions.assertEquals(InterestPaymentFrequencyUnit.WEEKLY, productMapper.mapInterestPayment(TimeUnit.W));
        Assertions.assertEquals(InterestPaymentFrequencyUnit.MONTHLY, productMapper.mapInterestPayment(TimeUnit.M));
        Assertions.assertEquals(InterestPaymentFrequencyUnit.YEARLY, productMapper.mapInterestPayment(TimeUnit.Y));
    }

    @Test
    public void mapLegalEntityId() {
        Assertions.assertNull(productMapper.mapLegalEntityId(null));

        List<LegalEntityReference> legalEntityReferenceList = List.of(buildLegalEntityReference("leid_1"), buildLegalEntityReference("leid_2"));
        List<String> legalEntityIdList = productMapper.mapLegalEntityId(legalEntityReferenceList);
        Assertions.assertEquals(legalEntityReferenceList.size(), legalEntityIdList.size());

        Assertions.assertNull(productMapper.mapLegalEntityReference(null));
        Assertions.assertEquals(legalEntityIdList.size(), productMapper.mapLegalEntityReference(legalEntityIdList).size());
    }

    @Test
    public void mapUserPreference() {
        Assertions.assertNull(productMapper.mapUserPreference(null));

        UserPreferences source = new UserPreferences()
                .userExternalId("user_ext_id")
                .visible(true)
                .alias("user_alias")
                .favorite(false)
                .putAdditionsItem("k1", "v1")
                .putAdditionsItem("k2", "v2");

        AccountUserPreferencesItemPut target = productMapper.mapUserPreference(source);
        Assertions.assertEquals(source.getUserExternalId(), target.getUserId());
        Assertions.assertEquals(source.getVisible(), target.getVisible());
        Assertions.assertEquals(source.getAlias(), target.getAlias());
        Assertions.assertEquals(source.getFavorite(), target.getFavorite());
        Assertions.assertNotNull(target.getAdditions());
        Assertions.assertEquals(source.getAdditions().size(), target.getAdditions().size());
    }

}
