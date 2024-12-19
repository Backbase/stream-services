package com.backbase.stream.compositions.product.core.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import org.junit.jupiter.api.Test;

class RequestConfigTest {

  @Test
  void isTransactionChainEnabled() {
    RequestConfig requestConfig = buiildRequestConfig(false, null);
    assertFalse(requestConfig.isTransactionChainEnabled().get());

    requestConfig = buiildRequestConfig(true, null);
    assertTrue(requestConfig.isTransactionChainEnabled().get());
  }

  @Test
  void isTransactionChainAsync() {
    RequestConfig requestConfig = buiildRequestConfig(null, false);
    assertFalse(requestConfig.isTransactionChainAsync().get());

    requestConfig = buiildRequestConfig(null, true);
    assertTrue(requestConfig.isTransactionChainAsync().get());
  }

  private RequestConfig buiildRequestConfig(Boolean enabled, Boolean async) {
    return RequestConfig.builder()
        .chains(
            RequestConfig.Chains.builder()
                .transactionComposition(
                    ProductConfigurationProperties.TransactionComposition.builder()
                        .enabled(enabled)
                        .async(async)
                        .build())
                .build())
        .build();
  }
}
