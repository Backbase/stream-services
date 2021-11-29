package com.backbase.stream.compositions.legalentity.core.mapper;

import com.backbase.compositions.integration.legalentity.api.service.v1.model.LegalEntity;
import org.mapstruct.Mapper;

@Mapper
public interface LegalEntityMapper {
    LegalEntity reMap(com.backbase.stream.legalentity.model.LegalEntity legalEntity);
    com.backbase.stream.legalentity.model.LegalEntity reMap(LegalEntity legalEntity);
}
