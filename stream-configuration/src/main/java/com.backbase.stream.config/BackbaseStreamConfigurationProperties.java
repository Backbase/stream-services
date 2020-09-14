package com.backbase.stream.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("backbase.stream")
@Data
@NoArgsConstructor
public class BackbaseStreamConfigurationProperties {


    private DbsConnectionProperties dbs;

    @Data
    @NoArgsConstructor
    public static class DbsConnectionProperties {
        /**
         * The location of DBS User Presentation Service.
         */
        private String userPresentationBaseUrl;

        /**
         * The location of Access Group Presentation Service.
         */
        private String accessGroupPresentationBaseUrl;


        /**
         * The location of Access Control PandP Service.
         */
        private String accessControlPandpBaseUrl;

        /**
         * The location of Legal Entity Presentation Service.
         */
        private String legalEntityPresentationBaseUrl;

        /**
         * The location of Accounts Presentation Service.
         */
        private String accountPresentationBaseUrl;

        /**
         * The location of Arrangement PandP
         */
        private String arrangementPandpBaseUrl;

        /**
         * Location of Transaction Presentation Service.
         */
        private String transactionPresentationBaseUrl;
        /**
         * Location of Limits Presentation Service.
         */
        private String limitsPresentationBaseUrl;

        /**
         * Location of Product Summary Service"
         */
        private String productSummaryBaseUrl;
    }


}
