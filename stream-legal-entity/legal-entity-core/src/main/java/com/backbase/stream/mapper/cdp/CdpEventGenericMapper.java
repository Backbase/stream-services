package com.backbase.stream.mapper.cdp;

import static java.util.Objects.nonNull;
import static org.mapstruct.ReportingPolicy.ERROR;

import com.backbase.cdp.ingestion.api.service.v1.model.CdpEvent;
import com.backbase.cdp.ingestion.api.service.v1.model.CdpEventMetadata;
import com.backbase.cdp.profiles.api.service.v1.model.ExternalId;
import com.backbase.stream.legalentity.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ERROR)
public interface CdpEventGenericMapper {

    String SOURCE_BACKBASE = "BACKBASE";
    String SOURCE_CORE_BANKING_SYSTEM = "CORE_SYSTEM";

    String TYPE_USER_ID = "USER_ID";
    String TYPE_CUSTOMER_ID = "CUSTOMER_ID";

    @Mapping(target = "eventId", expression = "java(generateEventId())")
    @Mapping(target = "timestamp", expression = "java(generateEventTimestamp())")
    @Mapping(target = "sessionId", ignore = true)
    @Mapping(target = "cdpCustomerId", ignore = true)
    @Mapping(target = "context", ignore = true)
    @Mapping(target = "metadata", expression = "java(generateEventMetadata(sourceService))")
    @Mapping(target = "eventType", source = "eventType")
    @Mapping(target = "sourceSystem", source = "sourceSystem")
    @Mapping(target = "sourceType", source = "sourceType")
    @Mapping(target = "sourceId", source = "sourceId")
    @Mapping(target = "data", expression = "java(mapEntityToData(object))")
    CdpEvent mapObjectToCdpEvent(String eventType,
                                 String sourceService,
                                 String sourceSystem,
                                 String sourceType,
                                 String sourceId,
                                 Object object);

    default Map<String, Object> mapEntityToData(Object entity) {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper.convertValue(entity, new TypeReference<>() {});
    }

    default String generateEventId() {
        return java.util.UUID.randomUUID().toString();
    }

    default OffsetDateTime generateEventTimestamp() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    default CdpEventMetadata generateEventMetadata(String eventSource) {
        return new CdpEventMetadata()
            .processedBy(List.of("stream-services"))
            .schemaVersion("1")
            .source(eventSource);
    }

    default List<ExternalId> mapUserToExternalIds(User user, String legalEntityInternalId, String legalEntityExternalId) {
        List<ExternalId> externalIds = new ArrayList<>();
        if (nonNull(user.getInternalId())) {
            externalIds.add(new ExternalId()
                .source(SOURCE_BACKBASE)
                .type(TYPE_USER_ID)
                .id(user.getInternalId())
                .verified(true));
        }
        if (nonNull(user.getExternalId())) {
            externalIds.add(new ExternalId()
                .source(SOURCE_CORE_BANKING_SYSTEM)
                .type(TYPE_USER_ID)
                .id(user.getExternalId())
                .verified(true));
        }
        if (nonNull(legalEntityInternalId)) {
            externalIds.add(new ExternalId()
                .source(SOURCE_BACKBASE)
                .type(TYPE_CUSTOMER_ID)
                .id(legalEntityInternalId)
                .verified(true));
        }
        if (nonNull(legalEntityExternalId)) {
            externalIds.add(new ExternalId()
                .source(SOURCE_CORE_BANKING_SYSTEM)
                .type(TYPE_CUSTOMER_ID)
                .id(legalEntityExternalId)
                .verified(true));
        }
        return externalIds;
    }

}
