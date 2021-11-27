package com.backbase.stream.compositions.legalentity;

import com.backbase.stream.compositions.legalentity.model.LegalEntityIngestPullRequest;
import com.backbase.stream.legalentity.model.LegalEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class LegalEntityIntegrationService {
    public LegalEntity retrieveLegalEntity(LegalEntityIngestPullRequest request) {
        return null;
    }
}
