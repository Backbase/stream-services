package com.backbase.stream.compositions.legalentity.core.model;

import com.backbase.stream.legalentity.model.IdentityUserLinkStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LegalEntityPullRequest {

  private String legalEntityExternalId;

  private String taxId;

  private String userExternalId;

  private String parentLegalEntityExternalId;

  private String realmName;

  private List<String> referenceJobRoleNames;

  private Boolean isAdmin;

  private IdentityUserLinkStrategy identityUserLinkStrategy;

  private Map<String, String> additions;

  private Boolean productChainEnabled;
}

