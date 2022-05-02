package com.backbase.stream.compositions.legalentity.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class LegalEntityPullRequest {
    private String legalEntityExternalId;
    private Map<String, String> additionalParameters;
}

