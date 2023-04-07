package com.backbase.stream.compositions.product.core.model;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProductIngestPullRequest {
  private String legalEntityExternalId;
  private String legalEntityInternalId;
  private String serviceAgreementExternalId;
  private String serviceAgreementInternalId;
  private String userExternalId;
  private String userInternalId;
  private List<String> referenceJobRoleNames;
  private List<String> membershipAccounts;
  private Map<String, String> additions;
  private String source;
  private Boolean transactionChainEnabled;
  private Boolean paymentOrderChainEnabled;
}
