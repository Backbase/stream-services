package com.backbase.stream.mappers;

import com.backbase.dbs.paymentorder.api.service.v2.model.GetPaymentOrderResponse;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPutRequest;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentOrderTypeMapper {

    PaymentOrderPutRequest mapPaymentOrderPostRequest(PaymentOrderPostRequest paymentOrderPostRequest);

    List<PaymentOrderPostRequest> mapPaymentOrderPostRequest(List<GetPaymentOrderResponse> paymentOrderPostRequest);
}
