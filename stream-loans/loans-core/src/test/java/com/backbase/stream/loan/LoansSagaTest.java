package com.backbase.stream.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.backbase.loan.inbound.api.service.v1.LoansApi;
import com.backbase.loan.inbound.api.service.v1.model.BatchResponseItemExtended;
import com.backbase.loan.inbound.api.service.v1.model.BatchUpsertLoans;
import com.backbase.loan.inbound.api.service.v1.model.InboundIntegrationArrangementAttributes;
import com.backbase.loan.inbound.api.service.v1.model.InboundIntegrationBorrower;
import com.backbase.loan.inbound.api.service.v1.model.InboundIntegrationCollateral;
import com.backbase.loan.inbound.api.service.v1.model.InboundIntegrationDebtAttributes;
import com.backbase.loan.inbound.api.service.v1.model.InboundIntegrationDefaultSettlementAccount;
import com.backbase.loan.inbound.api.service.v1.model.InboundIntegrationFrequency;
import com.backbase.loan.inbound.api.service.v1.model.InboundIntegrationInterestAttributes;
import com.backbase.loan.inbound.api.service.v1.model.InboundIntegrationLoan;
import com.backbase.loan.inbound.api.service.v1.model.InboundIntegrationLoanStatus;
import com.backbase.loan.inbound.api.service.v1.model.InboundIntegrationOverdueAttributes;
import com.backbase.loan.inbound.api.service.v1.model.InboundIntegrationPaymentAttributes;
import com.backbase.loan.inbound.api.service.v1.model.InboundIntegrationTermUnit;
import com.backbase.stream.legalentity.model.AvailableBalance;
import com.backbase.stream.legalentity.model.Collateral;
import com.backbase.stream.legalentity.model.CreditLimit;
import com.backbase.stream.legalentity.model.DocumentMetadata;
import com.backbase.stream.legalentity.model.FrequencyUnit;
import com.backbase.stream.legalentity.model.Loan;
import com.backbase.stream.legalentity.model.LoanStatus;
import com.backbase.stream.legalentity.model.TermUnit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class LoansSagaTest {

    @Mock
    private LoansApi loansApi;

    @InjectMocks
    private LoansSaga loansSaga;

    @Test
    void executeTask() throws ExecutionException, InterruptedException {
        Loan loanEmpty = new Loan();
        String internalId = "6c520d87-f736-403a-8679-b08c5f3d9a70";
        loanEmpty.setInternalId(internalId);
        String externalId = "fece07ee-bb19-4872-b0cc-f27efee17b9f";
        loanEmpty.setExternalId(externalId);
        Loan loanFull = createLoan();

        LoansTask streamTask = new LoansTask();
        streamTask.setData(List.of(loanEmpty, loanFull));
        ArgumentCaptor<BatchUpsertLoans> upsertLoansCaptor = ArgumentCaptor.forClass(
            BatchUpsertLoans.class);
        Mockito.when(loansApi.postBatchUpsertLoans(upsertLoansCaptor.capture()))
            .thenReturn(Flux.just(new BatchResponseItemExtended().arrangementId(internalId).resourceId(externalId)));

        loansSaga.executeTask(streamTask).toFuture().get();

        BatchUpsertLoans value = upsertLoansCaptor.getValue();
        Assertions.assertNotNull(value);
        Assertions.assertNotNull(value.getLoans());
        Assertions.assertEquals(2, value.getLoans().size());
        Assertions.assertNotNull(value.getLoans().get(0));
        Assertions.assertNotNull(value.getLoans().get(0).getArrangementAttributes());
        assertEquals(externalId, value.getLoans().get(0).getArrangementAttributes().getExternalId());
        assertEquals(internalId, value.getLoans().get(0).getArrangementAttributes().getInternalId());
        assertLoan(value.getLoans().get(1));
    }

    private void assertLoan(InboundIntegrationLoan loan) {
        InboundIntegrationArrangementAttributes arrangementAttributes = loan.getArrangementAttributes();
        assertEquals("0b72a53e-d8b8-45fe-b3d4-5fc62637ee0d", arrangementAttributes.getInternalId());
        assertEquals("2e41b450-1ef7-486b-b118-69481aa3624b", arrangementAttributes.getExternalId());
        assertEquals("USD", loan.getCurrencyCode());
        assertEquals(InboundIntegrationLoanStatus.ACTIVE, loan.getLoanStatus());
        assertEquals("LineOfCredit", loan.getLoanType());
        assertEquals("0001", loan.getBranchCode());
        assertEquals(new BigDecimal("100500.00000"), loan.getAvailableBalance());
        assertEquals(new BigDecimal("100501.00000"), loan.getCreditLimit());
        assertEquals(OffsetDateTime.parse("1991-08-24T16:22:00+00:00"), loan.getStartDateTime());
        assertEquals(OffsetDateTime.parse("1991-08-24T16:23:00+00:00"), loan.getEndDateTime());
        assertTrue(loan.getIsOverdue());
        assertTrue(loan.getRevolving());
        assertTrue(loan.getIsFullyRepaid());
        assertEquals(InboundIntegrationTermUnit.DAY, loan.getTermUnit());
        assertEquals(365, loan.getTermCount());
        assertEquals(InboundIntegrationTermUnit.YEAR, loan.getRemainingTermUnit());
        assertEquals(101, loan.getRemainingTermCount());
        assertEquals("v1", loan.getAdditions().get("k1"));

        InboundIntegrationDebtAttributes debtAttributes = loan.getDebtAttributes();
        assertNotNull(debtAttributes);
        assertEquals(OffsetDateTime.parse("1991-08-24T16:20:00+00:00"),
            debtAttributes.getCalculationPeriodStartDateTime());
        assertEquals(OffsetDateTime.parse("1991-08-24T04:20:00+00:00"),
            debtAttributes.getCalculationPeriodEndDateTime());
        assertEquals(new BigDecimal("4004.004"), debtAttributes.getOutstandingAmount());
        assertEquals(new BigDecimal("6006.006"), debtAttributes.getDrawnAmount());
        assertEquals(new BigDecimal("7007.007"), debtAttributes.getFeesDueAmount());
        assertEquals(new BigDecimal("7008.008"), debtAttributes.getInterestDueAmount());
        assertEquals(new BigDecimal("7009.009"), debtAttributes.getPaidAmount());
        assertEquals(new BigDecimal("5005.005"), debtAttributes.getTaxesOnInterestAmount());

        InboundIntegrationInterestAttributes interestAttributes = loan.getInterestAttributes();
        assertNotNull(interestAttributes);
        assertEquals("8008.008", interestAttributes.getInterestRate());
        assertEquals(InboundIntegrationFrequency.ANNUALLY, interestAttributes.getInterestPaymentFrequency());
        assertEquals(new BigDecimal("10.20"), interestAttributes.getTotalAnnualCostPercentage());

        InboundIntegrationOverdueAttributes overdueAttributes = loan.getOverdueAttributes();
        assertNotNull(overdueAttributes);
        assertEquals(new BigDecimal("9009.009"), overdueAttributes.getInArrearsAmount());
        assertEquals(OffsetDateTime.parse("1991-08-24T00:00:00+00:00"), overdueAttributes.getInArrearsDateTime());
        assertEquals(4, overdueAttributes.getOverduePaymentsCount());
        assertEquals(new BigDecimal("10000.01"), overdueAttributes.getOverduePenaltyAmount());
        assertEquals(new BigDecimal("11000.011"), overdueAttributes.getOverdueInterestAmount());
        assertEquals(new BigDecimal("12000.012"), overdueAttributes.getOverdueEscrowPaymentAmount());
        assertEquals(new BigDecimal("13000.013"), overdueAttributes.getOverduePrincipalPaymentAmount());
        assertEquals(new BigDecimal("14000.014"), overdueAttributes.getOverdueTaxesOnInterestAmount());
        assertEquals(new BigDecimal("15000.015"), overdueAttributes.getActualLatePaymentCommissionAmount());
        assertEquals(new BigDecimal("16000.016"), overdueAttributes.getContractLatePaymentCommissionAmount());
        assertEquals(new BigDecimal("17000.017"), overdueAttributes.getLatePaymentCommissionTaxesAmount());

        InboundIntegrationPaymentAttributes paymentAttributes = loan.getPaymentAttributes();
        assertNotNull(paymentAttributes);
        assertEquals(OffsetDateTime.parse("1991-08-24T16:21:00+00:00"), paymentAttributes.getNextRepaymentDateTime());
        assertEquals(new BigDecimal("18000.018"), paymentAttributes.getNextRepaymentAmount());
        assertEquals(InboundIntegrationFrequency.MONTHLY, paymentAttributes.getPaymentFrequency());
        assertEquals(42, paymentAttributes.getNumberOfPaymentsMade());
        assertEquals(142, paymentAttributes.getTotalNumberOfPayments());
        assertEquals(InboundIntegrationTermUnit.WEEK, paymentAttributes.getTotalDirectAmortizationUnit());
        assertEquals(new BigDecimal("19000.019"), paymentAttributes.getTotalDirectAmortization());
        assertEquals(InboundIntegrationTermUnit.MONTH, paymentAttributes.getTotalIndirectAmortizationUnit());
        assertEquals(new BigDecimal("20000.02"), paymentAttributes.getTotalIndirectAmortization());

        InboundIntegrationDefaultSettlementAccount defaultSettlementAccount = loan.getDefaultSettlementAccount();
        assertNotNull(defaultSettlementAccount);
        assertEquals("DEFSETACCNUM01", defaultSettlementAccount.getNumber());
        assertEquals("ACCHOLDNAME01", defaultSettlementAccount.getName());
        assertEquals("DEFSETACCINTID01", defaultSettlementAccount.getInternalId());
        assertEquals("DEFSETACCEXTID01", defaultSettlementAccount.getExternalId());

        List<InboundIntegrationBorrower> borrowers = loan.getBorrowers();
        assertNotNull(borrowers);
        assertEquals(1, borrowers.size());
        assertEquals(new InboundIntegrationBorrower().name("borrower01"), borrowers.get(0));

        List<InboundIntegrationCollateral> collaterals = loan.getCollaterals();
        assertNotNull(collaterals);
        assertEquals(1, collaterals.size());
        InboundIntegrationCollateral expectedCollateral = new InboundIntegrationCollateral()
            .currencyCode("USD")
            .type("property")
            .currentValue(new BigDecimal("21000.021"))
            .specification("SPECIFICATION")
            .nextRevaluationDateTime(OffsetDateTime.parse("1991-08-24T16:24:00+00:00"));
        assertEquals(expectedCollateral, collaterals.get(0));
    }

    private Loan createLoan() {
        Loan loan = new Loan();
        loan.setInternalId("0b72a53e-d8b8-45fe-b3d4-5fc62637ee0d");
        loan.setExternalId("2e41b450-1ef7-486b-b118-69481aa3624b");
        loan.setCurrency("USD");
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setType("LineOfCredit");
        loan.setBankBranchCode("0001");
        loan.setAvailableBalance(new AvailableBalance().currencyCode("USD").amount(new BigDecimal("100500.00000")));
        loan.setCreditLimit(new CreditLimit().currencyCode("USD").amount(new BigDecimal("100501.00000")));
        loan.setCalculationPeriodStartDateTime(OffsetDateTime.parse("1991-08-24T16:20:00+00:00"));
        loan.setCalculationPeriodEndDateTime(OffsetDateTime.parse("1991-08-24T04:20:00+00:00"));
        loan.setOutstandingPayment(new BigDecimal("4004.004"));
        loan.setOutstandingPrincipalAmount(new BigDecimal("6006.006"));
        loan.setFeesDueAmount(new BigDecimal("7007.007"));
        loan.setAccruedInterest(new BigDecimal("7008.008"));
        loan.setPaidAmount(new BigDecimal("7009.009"));
        loan.setTaxesOnInterestAmount(new BigDecimal("5005.005"));
        loan.setAccountInterestRate(new BigDecimal("8008.008"));
        loan.setInterestPaymentFrequency(FrequencyUnit.ANNUALLY);
        loan.setTotalAnnualCostPercentage(new BigDecimal("10.20"));
        loan.setAmountInArrear(new BigDecimal("9009.009"));
        loan.setOverdueSince(LocalDate.parse("1991-08-24"));
        loan.setOverduePaymentsCount(4);
        loan.setOverduePenaltyAmount(new BigDecimal("10000.01"));
        loan.setOverdueInterestAmount(new BigDecimal("11000.011"));
        loan.setOverdueEscrowPaymentAmount(new BigDecimal("12000.012"));
        loan.setOverduePrincipalPaymentAmount(new BigDecimal("13000.013"));
        loan.setOverdueTaxesOnInterestAmount(new BigDecimal("14000.014"));
        loan.setActualLatePaymentCommissionAmount(new BigDecimal("15000.015"));
        loan.setContractLatePaymentCommissionAmount(new BigDecimal("16000.016"));
        loan.setLatePaymentCommissionTaxesAmount(new BigDecimal("17000.017"));
        loan.setNextRepaymentDateTime(OffsetDateTime.parse("1991-08-24T16:21:00+00:00"));
        loan.setNextRepaymentAmount(new BigDecimal("18000.018"));
        loan.setPaymentFrequency(FrequencyUnit.MONTHLY);
        loan.setNumberOfPaymentsMade(42);
        loan.setTotalNumberOfPayments(142);
        loan.setTotalDirectAmortizationUnit(TermUnit.WEEKLY);
        loan.setTotalDirectAmortization(new BigDecimal("19000.019"));
        loan.setTotalIndirectAmortizationUnit(TermUnit.MONTHLY);
        loan.setTotalIndirectAmortization(new BigDecimal("20000.02"));
        loan.setAccountOpeningDate(OffsetDateTime.parse("1991-08-24T16:22:00+00:00"));
        loan.setMaturityDate(OffsetDateTime.parse("1991-08-24T16:23:00+00:00"));
        loan.setDefaultSettlementAccountNumber("DEFSETACCNUM01");
        loan.setAccountHolderName("ACCHOLDNAME01");
        loan.setDefaultSettlementAccountInternalId("DEFSETACCINTID01");
        loan.setDefaultSettlementAccountExternalId("DEFSETACCEXTID01");
        loan.setDocumentMetadatas(Collections.singletonList(new DocumentMetadata()
            .id("3b427323-8d5d-4863-a87f-57c94fb290d6")
            .name("docName")
            .contentType("application/json")));
        loan.setBorrowers(Collections.singletonList("borrower01"));
        loan.setIsOverdue(true);
        loan.setRevolving(true);
        loan.setIsFullyRepaid(true);
        loan.setTermUnit(TermUnit.DAILY);
        loan.setTermCount(365);
        loan.setRemainingTermUnit(TermUnit.QUARTERLY);
        loan.setRemainingTermCount(101);
        loan.setCollaterals(Collections.singletonList(
            new Collateral()
                .currencyCode("USD")
                .type("property")
                .currentValue(new BigDecimal("21000.021"))
                .specification("SPECIFICATION")
                .nextRevaluationDateTime(OffsetDateTime.parse("1991-08-24T16:24:00+00:00"))));
        loan.setAdditions(Map.of("k1", "v1"));
        return loan;
    }
}