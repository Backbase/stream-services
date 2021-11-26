package com.backbase.stream.compositions.legalentity.model;

import com.backbase.stream.legalentity.model.LegalEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class LegalEntityIngestPullRequest {
    private String eventId;
    private LegalEntity legalEntity;
}
