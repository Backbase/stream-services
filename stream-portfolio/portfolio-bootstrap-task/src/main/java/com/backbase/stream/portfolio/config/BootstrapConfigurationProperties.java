package com.backbase.stream.portfolio.config;

import com.backbase.stream.portfolio.model.WealthBundle;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "bootstrap", ignoreInvalidFields = true)
@Data
@NoArgsConstructor
public class BootstrapConfigurationProperties {

    private List<WealthBundle> wealthBundles;
}
