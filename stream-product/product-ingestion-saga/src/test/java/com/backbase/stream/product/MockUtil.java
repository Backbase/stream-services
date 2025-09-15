package com.backbase.stream.product;

import com.backbase.accesscontrol.datagroup.api.service.v1.model.DataGroup;
import com.backbase.accesscontrol.functiongroup.api.service.v1.model.FunctionGroupItem;
import com.backbase.stream.legalentity.model.AvailableBalance;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BookedBalance;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup.TypeEnum;
import com.backbase.stream.legalentity.model.CreditCard;
import com.backbase.stream.legalentity.model.CurrentAccount;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.LegalEntityReference;
import com.backbase.stream.legalentity.model.Loan;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.SavingsAccount;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.TermDeposit;
import com.backbase.stream.legalentity.model.TermUnit;
import com.backbase.stream.legalentity.model.User;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class MockUtil {


    @NotNull
    public static TermDeposit buildTermDeposit() {
        TermDeposit termDeposit = new TermDeposit()
            .termNumber(BigDecimal.valueOf(21212))
            .termUnit(TermUnit.DAILY)
            .BBAN("777151235")
            .accountHolderName("John Doe")
            .bookedBalance(new BookedBalance().amount(BigDecimal.valueOf(50)));
        termDeposit.externalId("termExtId").productTypeExternalId("Term Deposit").currency("GBP")
            .legalEntities(List.of(new LegalEntityReference().externalId("termInternalId")));
        return termDeposit;
    }

    @NotNull
    public static CreditCard buildCreditCard() {
        CreditCard creditCard = new CreditCard()
            .availableBalance(new AvailableBalance().amount(BigDecimal.valueOf(100)))
            .BBAN("777151236")
            .accountHolderName("John Doe")
            .bookedBalance(new BookedBalance().amount(BigDecimal.valueOf(50)));
        creditCard.externalId("ccExtId").productTypeExternalId("Credit Card").currency("GBP")
            .legalEntities(List.of(new LegalEntityReference().externalId("ccInternalId")));
        return creditCard;
    }

    @NotNull
    public static SavingsAccount buildSavingsAccount() {
        SavingsAccount savingsAccount = new SavingsAccount();
        savingsAccount.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP")
            .legalEntities(List.of(new LegalEntityReference().externalId("savInternalId")));
        return savingsAccount;
    }

    @NotNull
    public static CurrentAccount buildCurrentAccount() {
        CurrentAccount currentAccount = new CurrentAccount()
            .availableBalance(new AvailableBalance().amount(BigDecimal.valueOf(100)))
            .BBAN("777151234")
            .accountHolderName("John Doe")
            .bookedBalance(new BookedBalance().amount(BigDecimal.valueOf(50)));
        currentAccount.externalId("currentAccountExtId").productTypeExternalId("Current Account")
            .currency("GBP")
            .legalEntities(List.of(new LegalEntityReference().externalId("currInternalId")));
        return currentAccount;
    }

    @NotNull
    public static Loan buildLoanAccount() {
        Loan loan = new Loan().availableBalance(new AvailableBalance().amount(BigDecimal.valueOf(100)))
            .BBAN("777151238")
            .accountHolderName("John Doe")
            .bookedBalance(new BookedBalance().amount(BigDecimal.valueOf(50)));
        loan.externalId("loanAccountExtId").productTypeExternalId("Loan").currency("GBP")
            .legalEntities(List.of(new LegalEntityReference().externalId("loanInternalId")));
        return loan;
    }

    public static JobProfileUser buildJobProfile() {
        return new JobProfileUser().user(
                new User().internalId("someRegularUserInId")
                    .externalId("someRegularUserExId"))
            .legalEntityReference(new LegalEntityReference().externalId("someLeExternalId"))
            .referenceJobRoleNames(List.of("someJR"));
    }

    public static DataGroup buildDataGroupItem() {
        return new DataGroup().id("someDgItemExId1In").name("somePgName");
    }

    public static FunctionGroupItem buildFunctionGroupItem() {
        return new FunctionGroupItem().id("some-fg1").name("someJR").type(
            FunctionGroupItem.TypeEnum.CUSTOM);
    }

    public static List<BusinessFunctionGroup> buildBusinessFunctionGroupList() {
        return List.of(
            new BusinessFunctionGroup().name("someJR").id("some-fg1").type(TypeEnum.DEFAULT));
    }

    public static User buildUser() {
        return new User().internalId("someRegularUserInId")
            .externalId("someRegularUserExId");
    }

    public static ProductGroup createProductGroup() {

        SavingsAccount savingsAccount = buildSavingsAccount();
        CurrentAccount currentAccount = buildCurrentAccount();
        Loan loan = buildLoanAccount();
        TermDeposit termDeposit = buildTermDeposit();
        CreditCard creditCard = buildCreditCard();

        ProductGroup productGroup = new ProductGroup();
        productGroup.setServiceAgreement(
            new ServiceAgreement().internalId("sa_internalId").externalId("externalId"));

        productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS)
            .name("somePgName")
            .description("somePgDescription")
            .savingAccounts(Collections.singletonList(savingsAccount))
            .currentAccounts(Collections.singletonList(currentAccount))
            .loans(Collections.singletonList(loan))
            .termDeposits(Collections.singletonList(termDeposit))
            .creditCards(Collections.singletonList(creditCard))
            .users(List.of(buildJobProfile()));

        return productGroup;
    }
}