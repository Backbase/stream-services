package com.backbase.stream.worker.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@PropertySource("classpath:defaults.properties")
public class DefaultConfiguration {

}
