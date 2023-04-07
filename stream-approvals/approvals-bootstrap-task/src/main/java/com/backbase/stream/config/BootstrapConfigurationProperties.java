package com.backbase.stream.config;

import com.backbase.stream.approval.model.Approval;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import javax.validation.constraints.NotNull;

@Data
@Validated
@NoArgsConstructor
@ConfigurationProperties(prefix = "bootstrap", ignoreInvalidFields = true)
public class BootstrapConfigurationProperties {

    @NotNull private List<Approval> approvals;
}
