package com.backbase.stream.mappers;

import com.backbase.dbs.paymentorder.api.service.v3.model.GetPaymentOrderResponse;
import com.backbase.dbs.paymentorder.api.service.v3.model.PaymentOrderPostRequest;
import com.backbase.dbs.paymentorder.api.service.v3.model.PaymentOrderPutRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentOrderTypeMapper {

    @Mapping(source = "schedule.nextExecutionDate", target = "nextExecutionDate")
    PaymentOrderPutRequest mapPaymentOrderPostRequest(PaymentOrderPostRequest paymentOrderPostRequest);

    List<PaymentOrderPostRequest> mapPaymentOrderPostRequest(List<GetPaymentOrderResponse> paymentOrderPostRequest);
}
