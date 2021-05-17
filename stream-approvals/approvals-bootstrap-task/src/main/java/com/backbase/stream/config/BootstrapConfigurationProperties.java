package com.backbase.stream.config;

import com.backbase.stream.approval.model.Approval;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bootstrap", ignoreInvalidFields = true)
@Data
@NoArgsConstructor
public class BootstrapConfigurationProperties {

    private List<Approval> approvals;

}
