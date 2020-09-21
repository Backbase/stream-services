package com.backbase.stream.start.mvc;

import java.util.Optional;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

public class VersionHolder {

    private final String version;

    public VersionHolder(ApplicationContext context) {
        version = context.getBeansWithAnnotation(SpringBootApplication.class).entrySet().stream()
            .findFirst()
            .flatMap(es -> {
                final String implementationVersion = es.getValue().getClass().getPackage().getImplementationVersion();
                return Optional.ofNullable(implementationVersion);
            }).orElse("");
    }

    public String getVersion() {
        return version;
    }
}