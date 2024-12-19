package com.backbase.stream.compositions.paymentorders.core.mapper;

import java.util.List;

import com.backbase.dbs.paymentorder.api.service.v3.model.PaymentOrderPutResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

import com.backbase.dbs.paymentorder.api.service.v3.model.PaymentOrderPostRequest;
import com.backbase.dbs.paymentorder.api.service.v3.model.PaymentOrderPostResponse;
import com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderIngestionResponse;
import com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPullIngestionRequest;
import com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPushIngestionRequest;
import com.backbase.stream.compositions.paymentorder.integration.client.model.PullIngestionRequest;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestPullRequest;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestPushRequest;
import com.backbase.stream.model.response.DeletePaymentOrderIngestDbsResponse;
import com.backbase.stream.model.response.NewPaymentOrderIngestDbsResponse;
import com.backbase.stream.model.response.PaymentOrderIngestDbsResponse;
import com.backbase.stream.model.response.UpdatePaymentOrderIngestDbsResponse;

/**
 * This is a mapper for various Payment Order request and response objects used in:
 * - dbs model
 * - payment-order-composition-api
 * - payment-order-integration-api
 * <p>
 * All Payment Order request and response objects used in above modules have exactly same structures they are built
 * from the common /api folder.
 */
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
@Component
public interface PaymentOrderMapper {

    PaymentOrderPostRequest mapIntegrationToStream(
            com.backbase.stream.compositions.paymentorder.integration.client.model.PaymentOrderPostRequest source);

     com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPostResponse mapStreamNewPaymentOrderToComposition(PaymentOrderPostResponse source);

    @Mapping(target="id", source="bankReferenceId")
    com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPostResponse mapStreamUpdatePaymentOrderToComposition(PaymentOrderPutResponse source);

    PullIngestionRequest mapStreamToIntegration(PaymentOrderIngestPullRequest source);

    PaymentOrderIngestPullRequest mapPullRequest(PaymentOrderPullIngestionRequest source);

    PaymentOrderIngestPushRequest mapPushRequest(PaymentOrderPushIngestionRequest source);

    default PaymentOrderIngestionResponse mapPaymentOrderIngestionResponse(List<PaymentOrderIngestDbsResponse> paymentOrderIngestDbsResponses) {
        PaymentOrderIngestionResponse paymentOrderIngestionResponse = new PaymentOrderIngestionResponse();
        for (PaymentOrderIngestDbsResponse paymentOrderIngestDbsResponse : paymentOrderIngestDbsResponses) {
            if (paymentOrderIngestDbsResponse instanceof NewPaymentOrderIngestDbsResponse) {
                NewPaymentOrderIngestDbsResponse newPaymentOrderIngestDbsResponse = (NewPaymentOrderIngestDbsResponse) paymentOrderIngestDbsResponse;
                paymentOrderIngestionResponse.addNewPaymentOrderItem(
                    this.mapStreamNewPaymentOrderToComposition(
                        newPaymentOrderIngestDbsResponse.getPaymentOrderPostResponse()
                    )
                );
            } else if (paymentOrderIngestDbsResponse instanceof UpdatePaymentOrderIngestDbsResponse) {
                UpdatePaymentOrderIngestDbsResponse updatePaymentOrderIngestDbsResponse = (UpdatePaymentOrderIngestDbsResponse) paymentOrderIngestDbsResponse;
                paymentOrderIngestionResponse.addUpdatedPaymentOrderItem(
                    this.mapStreamUpdatePaymentOrderToComposition(
                        updatePaymentOrderIngestDbsResponse.getPaymentOrderPutResponse()
                    )
                );
            } else if (paymentOrderIngestDbsResponse instanceof DeletePaymentOrderIngestDbsResponse) {
                DeletePaymentOrderIngestDbsResponse deletePaymentOrderIngestDbsResponse = (DeletePaymentOrderIngestDbsResponse) paymentOrderIngestDbsResponse;
                paymentOrderIngestionResponse.addDeletedPaymentOrderItem(
                    deletePaymentOrderIngestDbsResponse.getPaymentOrderId()
                );
            }
        }
        return paymentOrderIngestionResponse;
    }
}
