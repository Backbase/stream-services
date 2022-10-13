package com.backbase.stream.clients.config;

import com.backbase.buildingblocks.webclient.client.ApiClientConfig;
import org.springframework.beans.factory.annotation.Value;

class DbsApiClientConfig extends ApiClientConfig {

    @Value("${backbase.communication.http.default-service-port:}")
    private Integer defaultServicePort;

    public DbsApiClientConfig(String serviceId) {
        super(serviceId);
    }

    @Override
    public String createBasePath() {
        if (getServicePort() == null && defaultServicePort != null) {
            return String.format("%s://%s:%s", getScheme(), getServiceId(), defaultServicePort);
        }
        return super.createBasePath();
    }
}
