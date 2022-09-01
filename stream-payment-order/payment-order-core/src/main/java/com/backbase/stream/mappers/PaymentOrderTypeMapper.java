package com.backbase.stream.mappers;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPutRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentOrderTypeMapper {

    PaymentOrderPutRequest mapPaymentOrderPostRequest(PaymentOrderPostRequest paymentOrderPostRequest);
}
