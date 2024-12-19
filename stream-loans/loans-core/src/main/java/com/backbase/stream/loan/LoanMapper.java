package com.backbase.stream.loan;

import com.backbase.loan.inbound.api.service.v1.model.InboundIntegrationBorrower;
import com.backbase.loan.inbound.api.service.v1.model.InboundIntegrationFrequency;
import com.backbase.loan.inbound.api.service.v1.model.InboundIntegrationLoan;
import com.backbase.loan.inbound.api.service.v1.model.InboundIntegrationLoanStatus;
import com.backbase.loan.inbound.api.service.v1.model.InboundIntegrationTermUnit;
import com.backbase.stream.legalentity.model.FrequencyUnit;
import com.backbase.stream.legalentity.model.Loan;
import com.backbase.stream.legalentity.model.LoanStatus;
import com.backbase.stream.legalentity.model.TermUnit;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ValueMapping;

@Mapper
public interface LoanMapper {

    @Mapping(target = "arrangementAttributes.internalId", source = "internalId")
    @Mapping(target = "arrangementAttributes.externalId", source = "externalId")
    @Mapping(target = "currencyCode", source = "currency")
    @Mapping(target = "loanStatus", source = "status")
    @Mapping(target = "loanType", source = "type")
    @Mapping(target = "branchCode", source = "bankBranchCode")
    @Mapping(target = "availableBalance", source = "availableBalance.amount")
    @Mapping(target = "creditLimit", source = "creditLimit.amount")
    @Mapping(target = "amortizationSchedule", ignore = true)
    @Mapping(target = "debtAttributes.calculationPeriodStartDateTime", source = "calculationPeriodStartDateTime")
    @Mapping(target = "debtAttributes.calculationPeriodEndDateTime", source = "calculationPeriodEndDateTime")
    @Mapping(target = "debtAttributes.outstandingAmount", source = "outstandingPayment")
    @Mapping(target = "debtAttributes.drawnAmount", source = "outstandingPrincipalAmount")
    @Mapping(target = "debtAttributes.feesDueAmount", source = "feesDueAmount")
    @Mapping(target = "debtAttributes.interestDueAmount", source = "accruedInterest")
    @Mapping(target = "debtAttributes.paidAmount", source = "paidAmount")
    @Mapping(target = "debtAttributes.taxesOnInterestAmount", source = "taxesOnInterestAmount")
    @Mapping(target = "interestAttributes.interestRate", source = "accountInterestRate")
    @Mapping(target = "interestAttributes.interestPaymentFrequency", source = "interestPaymentFrequency")
    @Mapping(target = "interestAttributes.totalAnnualCostPercentage", source = "totalAnnualCostPercentage")
    @Mapping(target = "overdueAttributes.inArrearsAmount", source = "amountInArrear")
    @Mapping(target = "overdueAttributes.inArrearsDateTime", expression = "java( toOffsetDateTime(loan.getOverdueSince()) )")
    @Mapping(target = "overdueAttributes.overduePaymentsCount", source = "overduePaymentsCount")
    @Mapping(target = "overdueAttributes.overduePenaltyAmount", source = "overduePenaltyAmount")
    @Mapping(target = "overdueAttributes.overdueInterestAmount", source = "overdueInterestAmount")
    @Mapping(target = "overdueAttributes.overdueEscrowPaymentAmount", source = "overdueEscrowPaymentAmount")
    @Mapping(target = "overdueAttributes.overduePrincipalPaymentAmount", source = "overduePrincipalPaymentAmount")
    @Mapping(target = "overdueAttributes.overdueTaxesOnInterestAmount", source = "overdueTaxesOnInterestAmount")
    @Mapping(target = "overdueAttributes.actualLatePaymentCommissionAmount", source = "actualLatePaymentCommissionAmount")
    @Mapping(target = "overdueAttributes.contractLatePaymentCommissionAmount", source = "contractLatePaymentCommissionAmount")
    @Mapping(target = "overdueAttributes.latePaymentCommissionTaxesAmount", source = "latePaymentCommissionTaxesAmount")
    @Mapping(target = "paymentAttributes.nextRepaymentDateTime", source = "nextRepaymentDateTime")
    @Mapping(target = "paymentAttributes.nextRepaymentAmount", source = "nextRepaymentAmount")
    @Mapping(target = "paymentAttributes.paymentFrequency", source = "paymentFrequency")
    @Mapping(target = "paymentAttributes.numberOfPaymentsMade", source = "numberOfPaymentsMade")
    @Mapping(target = "paymentAttributes.totalNumberOfPayments", source = "totalNumberOfPayments")
    @Mapping(target = "paymentAttributes.totalDirectAmortizationUnit", source = "totalDirectAmortizationUnit")
    @Mapping(target = "paymentAttributes.totalDirectAmortization", source = "totalDirectAmortization")
    @Mapping(target = "paymentAttributes.totalIndirectAmortizationUnit", source = "totalIndirectAmortizationUnit")
    @Mapping(target = "paymentAttributes.totalIndirectAmortization", source = "totalIndirectAmortization")
    @Mapping(target = "startDateTime", source = "accountOpeningDate")
    @Mapping(target = "endDateTime", source = "maturityDate")
    @Mapping(target = "defaultSettlementAccount.number", source = "defaultSettlementAccountNumber")
    @Mapping(target = "defaultSettlementAccount.name", source = "accountHolderName")
    @Mapping(target = "defaultSettlementAccount.internalId", source = "defaultSettlementAccountInternalId")
    @Mapping(target = "defaultSettlementAccount.externalId", source = "defaultSettlementAccountExternalId")
    InboundIntegrationLoan map(Loan loan);

    @ValueMapping(target = "DAY", source = "DAILY")
    @ValueMapping(target = "WEEK", source = "WEEKLY")
    @ValueMapping(target = "MONTH", source = "MONTHLY")
    @ValueMapping(target = "YEAR", source = "QUARTERLY")
    @ValueMapping(target = "YEAR", source = "YEARLY")
    InboundIntegrationTermUnit map(TermUnit termUnit);

    @ValueMapping(target = "WEEKLY", source = "WEEKLY")
    @ValueMapping(target = "BIWEEKLY", source = "BIWEEKLY")
    @ValueMapping(target = "TWICEMONTHLY", source = "TWICEMONTHLY")
    @ValueMapping(target = "MONTHLY", source = "MONTHLY")
    @ValueMapping(target = "FOURWEEKS", source = "FOURWEEKS")
    @ValueMapping(target = "BIMONTHLY", source = "BIMONTHLY")
    @ValueMapping(target = "QUARTERLY", source = "QUARTERLY")
    @ValueMapping(target = "SEMIANNUALLY", source = "SEMIANNUALLY")
    @ValueMapping(target = "ANNUALLY", source = "ANNUALLY")
    @ValueMapping(target = "MATURITY", source = "MATURITY")
    InboundIntegrationFrequency map(FrequencyUnit frequencyUnit);


    @Mapping(target = "name", expression = "java( name )")
    InboundIntegrationBorrower map(String name);

    List<InboundIntegrationBorrower> map(List<String> name);

    @ValueMapping(target = "ACTIVE", source = "ACTIVE")
    @ValueMapping(target = "INACTIVE", source = "INACTIVE")
    @ValueMapping(target = "PENDING", source = "PENDING")
    InboundIntegrationLoanStatus map(LoanStatus status);

    default OffsetDateTime toOffsetDateTime(LocalDate localDate) {
        return localDate != null ? localDate.atStartOfDay().atOffset(ZoneOffset.UTC) : null;
    }

}
