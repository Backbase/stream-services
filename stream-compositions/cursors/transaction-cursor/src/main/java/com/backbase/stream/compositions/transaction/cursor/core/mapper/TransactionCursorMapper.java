package com.backbase.stream.compositions.transaction.cursor.core.mapper;

import com.backbase.stream.compositions.transaction.cursor.core.domain.TransactionCursorEntity;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorResponse;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorUpsertRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Qualifier;
import org.mapstruct.ReportingPolicy;

/**
 * The Mapper for transforming Model to Domain & Entity to Domain Model
 */
@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TransactionCursorMapper {

    @Mapping(target = "cursor.id", source = "transactionCursorEntity.id")
    @Mapping(target = "cursor.arrangementId", source = "transactionCursorEntity.arrangementId")
    @Mapping(target = "cursor.extArrangementId", source = "transactionCursorEntity.extArrangementId")
    @Mapping(target = "cursor.lastTxnDate", source = "transactionCursorEntity.lastTxnDate")
    @Mapping(target = "cursor.lastTxnIds", source = "transactionCursorEntity.lastTxnIds", qualifiedBy = WithTxnModelParser.class)
    @Mapping(target = "cursor.legalEntityId", source = "transactionCursorEntity.legalEntityId")
    @Mapping(target = "cursor.additions", source = "transactionCursorEntity.additions", qualifiedBy = WithJsonToMap.class)
    @Mapping(target = "cursor.status", source = "transactionCursorEntity.status")
    TransactionCursorResponse mapToModel(TransactionCursorEntity transactionCursorEntity);

    @Mapping(target = "id", source = "transactionCursorUpsertRequest.cursor.id")
    @Mapping(target = "arrangementId", source = "transactionCursorUpsertRequest.cursor.arrangementId")
    @Mapping(target = "extArrangementId", source = "transactionCursorUpsertRequest.cursor.extArrangementId")
    @Mapping(target = "lastTxnIds", source = "transactionCursorUpsertRequest.cursor.lastTxnIds", qualifiedBy = WithTxnDomainParser.class)
    @Mapping(target = "legalEntityId", source = "transactionCursorUpsertRequest.cursor.legalEntityId")
    @Mapping(target = "additions", source = "transactionCursorUpsertRequest.cursor.additions", qualifiedBy = WithMapToJson.class)
    @Mapping(target = "status", source = "transactionCursorUpsertRequest.cursor.status")
    TransactionCursorEntity mapToDomain(
            TransactionCursorUpsertRequest transactionCursorUpsertRequest);


    List<TransactionCursorResponse> mapToListModel(
            List<TransactionCursorEntity> transactionCursorEntity);

    @Qualifier
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    @interface WithTxnModelParser {

    }

    @WithTxnModelParser
    default List<String> convertLastTransToListFormat(String lastTxnIds) {
        if (Objects.nonNull(lastTxnIds)) {
            return Stream.of(lastTxnIds.split(",")).collect(Collectors.toList());
        }
        return List.of();
    }

    @Qualifier
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    @interface WithTxnDomainParser {

    }

    @WithTxnDomainParser
    default String convertLastTransToStringFormat(List<String> lastTxnIds) {
        if (Objects.nonNull(lastTxnIds)) {
            return String.join(",", lastTxnIds);
        }
        return null;
    }

    @Qualifier
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    @interface WithJsonToMap {

    }

    @WithJsonToMap
    default Map<String, String> convertJsonToMapFormat(String additions)
            throws JsonProcessingException {
        if (Objects.nonNull(additions) && !additions.isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(additions, new TypeReference<>() {
            });
        }
        return null;
    }

    @Qualifier
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    @interface WithMapToJson {

    }

    @WithMapToJson
    default String convertMapToJsonFormat(Map<String, String> additions)
            throws JsonProcessingException {
        if (Objects.nonNull(additions) && !additions.isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(additions);
        }
        return null;
    }

}
