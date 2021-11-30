package com.backbase.stream.compositions.legalentity.core.mapper;

import com.backbase.stream.compositions.integration.legalentity.model.LegalEntity;
import org.mapstruct.Mapper;

@Mapper
public interface LegalEntityMapper {
    LegalEntity mapToIntegrationLegalEnity(com.backbase.stream.legalentity.model.LegalEntity legalEntity);
    com.backbase.stream.legalentity.model.LegalEntity mapToStreamLegalEntity(LegalEntity legalEntity);
    com.backbase.stream.compositions.legalentity.model.LegalEntity mapToCompostionLegalEntity(com.backbase.stream.legalentity.model.LegalEntity legalEntity);
}
