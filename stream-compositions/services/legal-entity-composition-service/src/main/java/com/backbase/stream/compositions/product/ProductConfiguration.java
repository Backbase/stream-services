package com.backbase.stream.compositions.product;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "backbase.stream.compositions.product")
public class ProductConfiguration {
}
