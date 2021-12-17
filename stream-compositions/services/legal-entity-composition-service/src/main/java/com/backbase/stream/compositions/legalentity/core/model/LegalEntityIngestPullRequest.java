package com.backbase.stream.compositions.legalentity.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LegalEntityIngestPullRequest {
    private String legalEntityExternalId;
}

