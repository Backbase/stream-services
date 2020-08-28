package com.backbase.stream.config.repository;

import org.springframework.cloud.config.server.environment.EnvironmentWatch;

public class KubernetesEnvironmentWatch implements EnvironmentWatch {

    @Override
    public String watch(String state) {
        return null;
    }
}
