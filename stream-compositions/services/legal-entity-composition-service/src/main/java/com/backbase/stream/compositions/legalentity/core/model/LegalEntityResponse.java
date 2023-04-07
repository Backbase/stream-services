package com.backbase.stream.compositions.legalentity.core.model;

import com.backbase.stream.legalentity.model.LegalEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LegalEntityResponse {

    private Boolean productChainEnabledFromRequest;
    private LegalEntity legalEntity;
    private List<String> membershipAccounts;
    private Map<String, String> additions;
}
