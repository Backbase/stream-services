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
        if (defaultServicePort == null) {
            return super.createBasePath();
        }

        return String.format("%s://%s:%s", getScheme(), getServiceId(), defaultServicePort);
    }
}
