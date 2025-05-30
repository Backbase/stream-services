package com.backbase.stream.clients.autoconfigure;

import com.backbase.stream.clients.config.AccessControlClientConfig;
import com.backbase.stream.clients.config.ApprovalClientConfig;
import com.backbase.stream.clients.config.ArrangementManagerClientConfig;
import com.backbase.stream.clients.config.ContactManagerClientConfig;
import com.backbase.stream.clients.config.CustomerProfileClientConfig;
import com.backbase.stream.clients.config.IdentityIntegrationClientConfig;
import com.backbase.stream.clients.config.InstrumentApiConfiguration;
import com.backbase.stream.clients.config.LimitsClientConfig;
import com.backbase.stream.clients.config.PaymentOrderClientConfig;
import com.backbase.stream.clients.config.PortfolioApiConfiguration;
import com.backbase.stream.clients.config.TransactionManagerClientConfig;
import com.backbase.stream.clients.config.UserManagerClientConfig;
import com.backbase.stream.clients.config.UserProfileManagerClientConfig;
import com.backbase.stream.clients.config.PlanManagerClientConfig;
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
    PlanManagerClientConfig.class,
    CustomerProfileClientConfig.class
})
@EnableConfigurationProperties
public class DbsApiClientsAutoConfiguration {

}
