package com.backbase.stream.compositions.legalentity.core.config;

import com.backbase.stream.legalentity.model.IdentityUserLinkStrategy;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "backbase.stream.compositions.legal-entity.gc.party")
public class GrandCentralPartyDefaultProperties {

    private String realmName;
    private String parentExternalId;
    private IdentityUserLinkStrategy identityUserLinkStrategy;
    private List<String> referenceJobRoleNames;
}
