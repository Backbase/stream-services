package com.backbase.stream.mappers;

import com.backbase.dbs.paymentorder.api.service.v3.model.PaymentOrderPostRequest;
import com.backbase.dbs.paymentorder.api.service.v3.model.PaymentOrderPutRequest;
import com.backbase.stream.common.PaymentOrderBaseTest;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.CollectionUtils;

public class PaymentOrderTypeMapperTest extends PaymentOrderBaseTest {

  @Test
  public void map_PaymentOrderPostRequest_To_PaymentOrderPutRequest() {
    PaymentOrderPostRequest source = paymentOrderPostRequest.get(0);
    PaymentOrderPutRequest target = buildPaymentOrderPutRequest(source);
    Assertions.assertEquals(source.getBankReferenceId(), target.getBankReferenceId());
    Assertions.assertEquals(source.getPaymentSetupId(), target.getPaymentSetupId());
    Assertions.assertEquals(source.getPaymentSubmissionId(), target.getPaymentSubmissionId());
    Assertions.assertEquals(source.getBankStatus(), target.getBankStatus());
    Assertions.assertEquals(source.getReasonCode(), target.getReasonCode());
    Assertions.assertEquals(source.getReasonText(), target.getReasonText());
    Assertions.assertEquals(source.getErrorDescription(), target.getErrorDescription());
    Assertions.assertEquals(source.getStatus(), target.getStatus());
    Assertions.assertEquals(source.getRequestedExecutionDate(), target.getRequestedExecutionDate());
    Assertions.assertNotNull(source.getSchedule());
    Assertions.assertEquals(
        source.getSchedule().getNextExecutionDate(), target.getNextExecutionDate());
    if (!CollectionUtils.isEmpty(source.getAdditions())) {
      Assertions.assertNotNull(target.getAdditions());
      Assertions.assertEquals(source.getAdditions().size(), target.getAdditions().size());
    }
  }

  @Test
  public void map_GetPaymentOrderResponse_To_PaymentOrderPostRequest() {
    Assertions.assertEquals(getPaymentOrderResponse.size(), paymentOrderPostRequest.size());
    AtomicInteger idx = new AtomicInteger(0);
    getPaymentOrderResponse.forEach(
        source -> {
          PaymentOrderPostRequest target = paymentOrderPostRequest.get(idx.get());
          Assertions.assertEquals(source.getId(), target.getId());
          Assertions.assertEquals(source.getBankReferenceId(), target.getBankReferenceId());
          Assertions.assertEquals(source.getInternalUserId(), target.getInternalUserId());
          Assertions.assertEquals(source.getPaymentSetupId(), target.getPaymentSetupId());
          Assertions.assertEquals(source.getPaymentSubmissionId(), target.getPaymentSubmissionId());
          Assertions.assertEquals(source.getApprovalId(), target.getApprovalId());
          Assertions.assertEquals(source.getBankStatus(), target.getBankStatus());
          Assertions.assertEquals(source.getReasonCode(), target.getReasonCode());
          Assertions.assertEquals(source.getReasonText(), target.getReasonText());
          Assertions.assertEquals(source.getErrorDescription(), target.getErrorDescription());
          Assertions.assertNotNull(source.getOriginator());
          Assertions.assertNotNull(target.getOriginator());
          Assertions.assertEquals(
              source.getOriginator().getName(), target.getOriginator().getName());
          Assertions.assertNotNull(source.getOriginatorAccount());
          Assertions.assertNotNull(target.getOriginatorAccount());
          Assertions.assertEquals(
              source.getOriginatorAccount().getArrangementId(),
              target.getOriginatorAccount().getArrangementId());
          Assertions.assertEquals(source.getTotalAmount(), target.getTotalAmount());
          Assertions.assertEquals(source.getInstructionPriority(), target.getInstructionPriority());
          Assertions.assertEquals(source.getStatus(), target.getStatus());
          Assertions.assertEquals(
              source.getRequestedExecutionDate(), target.getRequestedExecutionDate());
          Assertions.assertEquals(source.getPaymentMode(), target.getPaymentMode());
          Assertions.assertEquals(source.getPaymentType(), target.getPaymentType());
          Assertions.assertEquals(source.getEntryClass(), target.getEntryClass());
          Assertions.assertEquals(source.getSchedule(), target.getSchedule());
          Assertions.assertNotNull(source.getTransferTransactionInformation());
          Assertions.assertNotNull(target.getTransferTransactionInformation());
          Assertions.assertEquals(
              source.getTransferTransactionInformation().getTransferFee(),
              target.getTransferTransactionInformation().getTransferFee());
          Assertions.assertEquals(source.getCreatedBy(), target.getCreatedBy());
          Assertions.assertEquals(source.getCreatedAt(), target.getCreatedAt());
          Assertions.assertEquals(source.getUpdatedBy(), target.getUpdatedBy());
          Assertions.assertEquals(source.getUpdatedAt(), target.getUpdatedAt());
          Assertions.assertEquals(source.getIntraLegalEntity(), target.getIntraLegalEntity());
          Assertions.assertEquals(source.getServiceAgreementId(), target.getServiceAgreementId());
          Assertions.assertEquals(
              source.getOriginatorAccountCurrency(), target.getOriginatorAccountCurrency());
          Assertions.assertEquals(source.getConfirmationId(), target.getConfirmationId());
          if (!CollectionUtils.isEmpty(source.getAdditions())) {
            Assertions.assertNotNull(target.getAdditions());
            Assertions.assertEquals(source.getAdditions().size(), target.getAdditions().size());
          }

          idx.set(idx.get() + 1);
        });
  }
}
