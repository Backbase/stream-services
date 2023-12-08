package com.backbase.stream.compositions.product.core.mapper;

import com.backbase.stream.compositions.events.ingress.event.spec.v1.ProductPullEvent;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.TransactionsPullEvent;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.transaction.client.model.TransactionPullIngestionRequest;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface EventRequestsMapper {

    ProductIngestPullRequest map(ProductPullEvent event);

    TransactionsPullEvent map(TransactionPullIngestionRequest request);

    default ZonedDateTime map(OffsetDateTime value) {
        if (value != null) {
            return value.toZonedDateTime();
        }
        return null;
    }

}
