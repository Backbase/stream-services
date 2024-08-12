package com.backbase.stream.clients.autoconfigure;

import com.backbase.stream.clients.config.*;
import com.backbase.stream.context.config.ContextPropagationConfiguration;
import com.backbase.stream.webclient.configuration.DbsWebClientConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * This autoconfiguration class will set up all Banking Services clients according to the standards defined by the
 * Backbase SDK in the Community documentation.
 *
 * @see <a href="https://community.backbase.com/documentation/ServiceSDK/latest/generate_clients_from_openapi>Generate
 * clients from OpenAPI specs</a>
 */
@Configuration
@Import({
    DbsWebClientConfiguration.class,
    ContextPropagationConfiguration.class,
    AccessControlClientConfig.class,
    ApprovalClientConfig.class,
    ArrangementManagerClientConfig.class,
    ContactManagerClientConfig.class,
    IdentityIntegrationClientConfig.class,
    LimitsClientConfig.class,
    PaymentOrderClientConfig.class,
    TransactionManagerClientConfig.class,
    UserManagerClientConfig.class,
    UserProfileManagerClientConfig.class,
    InstrumentApiConfiguration.class,
    PortfolioApiConfiguration.class,
    PlanManagerClientConfig.class
})
@EnableConfigurationProperties
public class DbsApiClientsAutoConfiguration {

}
