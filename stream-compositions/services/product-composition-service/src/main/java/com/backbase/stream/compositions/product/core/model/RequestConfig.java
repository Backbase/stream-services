package com.backbase.stream.compositions.product.core.model;

import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestConfig {

  private Chains chains;

  public Optional<Boolean> isTransactionChainEnabled() {
    return chains == null
            || chains.getTransactionComposition() == null
            || chains.getTransactionComposition().getEnabled() == null
        ? Optional.empty()
        : Optional.of(Boolean.TRUE.equals(chains.getTransactionComposition().getEnabled()));
  }

  public Optional<Boolean> isTransactionChainAsync() {
    return chains == null
            || chains.getTransactionComposition() == null
            || chains.getTransactionComposition().getAsync() == null
        ? Optional.empty()
        : Optional.of(Boolean.TRUE.equals(chains.getTransactionComposition().getAsync()));
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Chains {

    private ProductConfigurationProperties.TransactionComposition transactionComposition;
  }

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  public abstract static class BaseComposition {

    private Boolean enabled = Boolean.FALSE;
    private Boolean async = Boolean.FALSE;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @SuperBuilder
  public static class TransactionComposition
      extends ProductConfigurationProperties.BaseComposition {

    private List<String> excludeProductTypeExternalIds = new ArrayList<>();
  }
}
