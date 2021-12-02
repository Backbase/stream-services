package com.backbase.stream.compositions.legalentity.core.model;

import com.backbase.stream.compositions.legalentity.core.RequestSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class LegalEntityIngestPullRequest {
    private String legalEntityExternalId;
    private Map<String, String> parameters;
}

