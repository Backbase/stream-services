package com.backbase.stream.compositions.paymentorders.core.mapper;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostResponse;
import com.backbase.dbs.paymentorder.api.service.v2.model.UpdateStatusPut;
import com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPullIngestionRequest;
import com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPushIngestionRequest;
import com.backbase.stream.compositions.paymentorder.integration.client.model.PullIngestionRequest;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestPullRequest;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestPushRequest;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;


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
    com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPostResponse mapStreamUpdatePaymentOrderToComposition(UpdateStatusPut source);

    PullIngestionRequest mapStreamToIntegration(PaymentOrderIngestPullRequest source);

    PaymentOrderIngestPullRequest mapPullRequest(PaymentOrderPullIngestionRequest source);

    PaymentOrderIngestPushRequest mapPushRequest(PaymentOrderPushIngestionRequest source);
}
