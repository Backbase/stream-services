package com.backbase.stream.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("backbase.stream")
@Data
@NoArgsConstructor
public class BackbaseStreamConfigurationProperties {

    private DbsConnectionProperties dbs;
    private IdentityConnectionProperties identity;

    @Data
    @NoArgsConstructor
    public static class DbsConnectionProperties {

        /**
         * The location of Access Group Presentation Service.
         */
        private String accessControlBaseUrl  = "http://access-control:8080";

        /**
         * The location of Accounts Presentation Service.
         */
        private String arrangementManagerBaseUrl = "http://arrangement-manager:8080";

        /**
         * Location of Transaction Presentation Service.
         */
        private String transactionManagerBaseUrl = "http://transaction-manager:8080";

        /**
         * Location of Limits Presentation Service.
         */
        private String limitsManagerBaseUrl = "http://limits-manager:8080";

        /**
         * The location of DBS User Presentation Service.
         */
        private String userManagerBaseUrl = "http://user-manager:8080";

        /**
         * The location of DBS User Profile Manager Service.
         */
        private String userProfileManagerBaseUrl = "http://user-profile-manager:8080";

    }

    @Data
    @NoArgsConstructor
    public static class IdentityConnectionProperties {
        /**
         * The location of Identity Service.
         */
        private String identityIntegrationBaseUrl = "http://identity-integration-service:8080";
    }

}
