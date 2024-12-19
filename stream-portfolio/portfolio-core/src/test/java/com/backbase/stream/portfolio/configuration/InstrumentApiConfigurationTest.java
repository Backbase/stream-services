package com.backbase.stream.portfolio.configuration;

import com.backbase.portfolio.instrument.integration.api.service.ApiClient;
import com.backbase.stream.clients.config.InstrumentApiConfiguration;
import org.junit.jupiter.api.Test;

class InstrumentApiConfigurationTest
    extends BaseApiConfigurationTest<InstrumentApiConfiguration, ApiClient> {

  @Override
  public ApiClient getClient() {
    return getConfig().instrumentApiClient(objectMapper, dateFormat);
  }

  @Override
  public InstrumentApiConfiguration getConfig() {
    return new InstrumentApiConfiguration();
  }

  @Override
  public String getBasePath(ApiClient apiClient) {
    return apiClient.getBasePath();
  }

  @Test
  void instrumentManagementApi() {
    assertBaseUrl(getConfig().instrumentManagementApi(getClient()).getApiClient());
  }

  @Test
  void instrumentPriceManagementApi() {
    assertBaseUrl(getConfig().instrumentPriceManagementApi(getClient()).getApiClient());
  }

  @Test
  void instrumentRegionManagementApi() {
    assertBaseUrl(getConfig().instrumentRegionManagementApi(getClient()).getApiClient());
  }

  @Test
  void instrumentCountryManagementApi() {
    assertBaseUrl(getConfig().instrumentCountryManagementApi(getClient()).getApiClient());
  }

  @Test
  void instrumentAssetClassManagementApi() {
    assertBaseUrl(getConfig().instrumentAssetClassManagementApi(getClient()).getApiClient());
  }

  @Test
  void instrumentApiClient() {
    assertBaseUrl(getConfig().instrumentApiClient(objectMapper, dateFormat));
  }
}
