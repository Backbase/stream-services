package com.backbase.stream.compositions.legalentity.core.model;

import com.backbase.stream.legalentity.model.LegalEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class LegalEntityIngestResponse {
    private final List<LegalEntity> legalEntities;
}