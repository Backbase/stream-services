package com.backbase.stream.compositions.paymentorders.core.mapper;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostResponse;
import com.backbase.dbs.paymentorder.api.service.v2.model.UpdateStatusPut;
import com.backbase.stream.compositions.paymentorder.integration.client.model.PullIngestionRequest;
import com.backbase.stream.compositions.paymentorders.core.model.PaymentOrderIngestPullRequest;
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

    /**
     * Maps composition PaymentOrderPostRequestBody to dbs PaymentOrderPostRequestBody model.
     *
     * @param PaymentOrderPostRequest
     * @return DBS Payment Order
     */
    PaymentOrderPostRequest mapIntegrationToStream(
            com.backbase.stream.compositions.paymentorder.integration.client.model.PaymentOrderPostRequest paymentOrderPostRequest);

     com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPostResponse mapStreamNewPaymentOrderToComposition(PaymentOrderPostResponse paymentOrderPostResponse);

    @Mapping(target="id", source="bankReferenceId")
    com.backbase.stream.compositions.paymentorder.api.model.PaymentOrderPostResponse mapStreamUpdatePaymentOrderToComposition(UpdateStatusPut paymentOrderPostResponse);

    /**
     * Maps Stream Pull Ingestion Request to Integration.
     *
     * @param model Stream Pull Ingestion Request
     * @return Integration Pull Ingestion Request
     */
    PullIngestionRequest mapStreamToIntegration(PaymentOrderIngestPullRequest model);
}
