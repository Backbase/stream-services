package com.backbase.stream.common;

import com.backbase.dbs.paymentorder.api.service.v2.model.*;
import com.backbase.stream.mappers.PaymentOrderTypeMapper;
import java.math.BigDecimal;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class PaymentOrderBaseTest {
    protected final PaymentOrderTypeMapper paymentOrderTypeMapper = Mappers.getMapper(PaymentOrderTypeMapper.class);

    protected final List<GetPaymentOrderResponse> getPaymentOrderResponse = buildGetPaymentOrderResponse();
    protected final List<PaymentOrderPostRequest> paymentOrderPostRequest = paymentOrderTypeMapper.mapPaymentOrderPostRequest(getPaymentOrderResponse);

    protected PaymentOrderPutRequest buildPaymentOrderPutRequest(PaymentOrderPostRequest source) {
        return paymentOrderTypeMapper.mapPaymentOrderPostRequest(source);
    }

    List<GetPaymentOrderResponse> buildGetPaymentOrderResponse() {
        List<GetPaymentOrderResponse> getPaymentOrderResponseList = new ArrayList<>();
        for(int idx=1; idx<=2; idx++) {
            SimpleInvolvedParty involvedParty = new SimpleInvolvedParty()
                    .name("name_" + idx)
                    .recipientId("recipientId_" + idx)
                    .role(InvolvedPartyRole.CREDITOR);

            SimpleOriginatorAccount originatorAccount = new SimpleOriginatorAccount()
                    .arrangementId("arrangementId_" + idx)
                    .externalArrangementId("externalArrangementId_" + idx)
                    .identification(new Identification().identification("identification_" + idx));

            SimpleSchedule schedule = new SimpleSchedule()
                    .on(idx)
                    .startDate(LocalDate.of(2023, 3, idx))
                    .endDate(LocalDate.of(2023, 3, idx))
                    .nextExecutionDate(LocalDate.of(2023, 3, idx))
                    .transferFrequency(SimpleSchedule.TransferFrequencyEnum.ONCE);

            GetPaymentOrderResponse getPaymentOrderResponse = new GetPaymentOrderResponse()
                    .id("id_" + idx)
                    .bankReferenceId("bankReferenceId_" + idx)
                    .bankReferenceId("bankReferenceId_" + idx)
                    .internalUserId("internalUserId" + idx)
                    .paymentSetupId("paymentSetupId_" + idx)
                    .approvalId("approvalId_" + idx)
                    .bankStatus("bankStatus_" + idx)
                    .reasonCode("reasonCode_" + idx)
                    .reasonText("reasonText_" + idx)
                    .errorDescription("errorDescription_" + idx)
                    .originator(involvedParty)
                    .originatorAccount(originatorAccount)
                    .totalAmount(new Currency().amount(BigDecimal.valueOf(idx)))
                    .instructionPriority(InstructionPriority.NORM)
                    .status(Status.ACCEPTED)
                    .requestedExecutionDate(LocalDate.of(2023, 3, idx))
                    .paymentMode(PaymentMode.SINGLE)
                    .paymentType("paymentType_" + idx)
                    .entryClass("entryClass_" + idx)
                    .schedule(schedule)
                    .transferTransactionInformation(new SimpleTransaction().transferFee(new Currency().amount(BigDecimal.valueOf(idx))))
                    .createdBy("createdBy_" + idx)
                    .createdAt("createdAt_" + idx)
                    .updatedBy("updatedBy_" + idx)
                    .updatedAt("updatedAt_" + idx)
                    .intraLegalEntity(true)
                    .serviceAgreementId("serviceAgreementId_" + idx)
                    .originatorAccountCurrency("originatorAccountCurrency_" + idx)
                    .confirmationId("confirmationId_" + idx)
                    .putAdditionsItem("key_" + idx, "value_" + idx);

            getPaymentOrderResponseList.add(getPaymentOrderResponse);
        }
        return getPaymentOrderResponseList;
    }

    protected static WebClientResponseException buildWebClientResponseException(HttpStatus httpStatus, String statusText) {
        return WebClientResponseException.create(httpStatus.value(), statusText, null, null, null);
    }

}
