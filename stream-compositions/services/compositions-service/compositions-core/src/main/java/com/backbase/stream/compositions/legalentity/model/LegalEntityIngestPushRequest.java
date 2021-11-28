package com.backbase.stream.compositions.legalentity.model;

import com.backbase.stream.legalentity.model.LegalEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LegalEntityIngestPushRequest {
    private String eventId;
    private LegalEntity legalEntity;
}
