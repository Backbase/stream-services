package com.backbase.stream.compositions.legalentity.core.config;

import com.backbase.stream.legalentity.model.IdentityUserLinkStrategy;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("backbase.stream.compositions.legal-entity.gc-defaults.party")
public class GrandCentralPartyDefaultProperties {

    private String realmName;
    private String parentExternalId;
    private List<String> referenceJobRoleNames;
    private IdentityUserLinkStrategy identityUserLinkStrategy = IdentityUserLinkStrategy.CREATE_IN_IDENTITY;
}
