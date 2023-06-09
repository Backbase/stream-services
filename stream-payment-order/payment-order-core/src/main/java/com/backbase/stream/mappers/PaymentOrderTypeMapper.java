package com.backbase.stream.mappers;

import com.backbase.dbs.paymentorder.api.service.v2.model.GetPaymentOrderResponse;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPutRequest;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentOrderTypeMapper {

    @Mapping(source = "schedule.nextExecutionDate", target = "nextExecutionDate")
    PaymentOrderPutRequest mapPaymentOrderPostRequest(
        PaymentOrderPostRequest paymentOrderPostRequest);

    List<PaymentOrderPostRequest> mapPaymentOrderPostRequest(
        List<GetPaymentOrderResponse> paymentOrderPostRequest);
}
