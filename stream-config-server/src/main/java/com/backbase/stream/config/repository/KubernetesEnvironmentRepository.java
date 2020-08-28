package com.backbase.stream.config.repository;

import com.google.common.collect.Maps;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.core.Ordered;
import org.springframework.util.Base64Utils;

@Slf4j

public class KubernetesEnvironmentRepository implements EnvironmentRepository, Ordered {

    public static final String STREAM_CLOUD_CONFIG = "stream/config";
    public static final String SHARED = "shared";
    private final KubernetesClient kubernetesClient;

    public KubernetesEnvironmentRepository(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public Environment findOne(String application, String profile, String label) {
        log.info("Getting environment properties for application: {} with profile: {} and label: {}", application, profile, label);
        Environment environment = new Environment(application, profile);

        log.debug("Retrieving shared config maps with the label: {}", STREAM_CLOUD_CONFIG);
        ConfigMapList shared = kubernetesClient.configMaps()
            .withLabel(STREAM_CLOUD_CONFIG, SHARED)
            .list();

        if (shared != null) {
            shared.getItems().forEach(configMap -> {
                PropertySource propertySource = getPropertySourceFrom(profile, configMap);
                if (propertySource != null)
                    environment.add(propertySource);
            });
        }

        ConfigMapList applicationConfigMaps = kubernetesClient.configMaps()
            .withLabel(STREAM_CLOUD_CONFIG, application)
            .list();

        if (applicationConfigMaps != null) {
            applicationConfigMaps.getItems().forEach(configMap -> {
                PropertySource propertySource = getPropertySourceFrom(profile, configMap);
                if (propertySource != null)
                    environment.add(propertySource);
            });
        }

        ConfigMap configMap = kubernetesClient.configMaps()
            .withName(application)
            .get();

        if (configMap != null) {
            PropertySource propertySource = getPropertySourceFrom(profile, configMap);
            environment.add(propertySource);
        }

        log.debug("Returning environment: {}", environment);
        return environment;
    }

    private PropertySource getPropertySourceFrom(String profile, ConfigMap configMap) {
        log.debug("Getting properties from ConfigMap: {}", configMap.getMetadata().getName());
        PropertySource propertySource = null;
        // Check if config map has application.properties or application.yaml or application.yml file
        if (configMap.getData().containsKey("application.properties")) {
            propertySource = searchProperties(configMap.getMetadata().getName(), profile, configMap.getData().get("application.properties"));
        } else if (configMap.getData().containsKey("application.yml")) {
            propertySource = searchYaml(configMap.getMetadata().getName(), profile, configMap.getData().get("application.yml"));
        } else if (configMap.getData().containsKey("application.yaml")) {
            propertySource = searchYaml(configMap.getMetadata().getName(), profile, configMap.getData().get("application.yaml"));
        } else {
            log.warn("No properties found in ConfigMap: {} with names application.properties, application.yml or application.yaml", configMap.getMetadata().getName());
        }

        // Sort the properties?
        return new PropertySource(profile, new TreeMap<>(propertySource.getSource()));
    }

    private PropertySource getPropertySourceFrom(String profile, Secret secret) {
        log.debug("Getting properties from Secret: {}", secret.getMetadata().getName());
        PropertySource propertySource = null;
        String encodedProperties = null;
        // Check if config map has application.properties or application.yaml or application.yml file
        if (secret.getData().containsKey("application.properties")) {
            encodedProperties = secret.getData().get("application.properties");
            String decoded = new String(Base64Utils.decodeFromString(encodedProperties));
            propertySource = searchProperties(secret.getMetadata().getName(), profile, decoded);
        } else if (secret.getData().containsKey("application.yml")) {
            encodedProperties = secret.getData().get("application.yml");
            String decoded = new String(Base64Utils.decodeFromString(encodedProperties));
            propertySource = searchYaml(secret.getMetadata().getName(), profile, decoded);
        } else if (secret.getData().containsKey("application.yaml")) {
            encodedProperties = secret.getData().get("application.yaml");
            String decoded = new String(Base64Utils.decodeFromString(encodedProperties));
            propertySource = searchYaml(secret.getMetadata().getName(), profile, decoded);
        } else {
            log.warn("No properties found in ConfigMap: {} with names application.properties, application.yml or application.yaml", secret.getMetadata().getName());
        }
        return propertySource;
    }

    private PropertySource searchProperties(String application, String profile, String data) {
        Environment environment = new Environment(application, profile);
        Properties props = new Properties();

        try {
            props.load(new StringReader(data));
        } catch (IOException e) {
            log.error("Failed to load properties from data: {}", data);
            return null;
        }

        Map<String, String> properties = Maps.fromProperties(props);

        PropertySource propertySource = new PropertySource(application, properties);
        environment.add(propertySource);

        return propertySource;
    }

    private PropertySource searchYaml(String application, String profile, String data) {
        Environment environment = new Environment(application, profile);

        YamlFileConfiguration yamlFileConfiguration = new YamlFileConfiguration();
        yamlFileConfiguration.load(new StringReader(data));

        Map<String, String> properties = yamlFileConfiguration.getProperties();

        PropertySource propertySource = new PropertySource(application, properties);
        environment.add(propertySource);
        return propertySource;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
