package com.backbase.stream.compositions.legalentity;

import com.backbase.stream.compositions.legalentity.model.LegalEntityIngestPullRequest;
import com.backbase.stream.compositions.legalentity.model.LegalEntityIngestPullResponse;
import org.springframework.stereotype.Service;

@Service
public class LegalEntityIngestionService {
    private LegalEntityConfiguration legalEntityConfiguration;

    public LegalEntityIngestionService(LegalEntityConfiguration legalEntityConfiguration) {
        this.legalEntityConfiguration = legalEntityConfiguration;
    }

    public LegalEntityIngestPullResponse ingest(LegalEntityIngestPullRequest ingestPullRequest) {
        return null;
    }
}
