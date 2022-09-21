package com.backbase.stream;

import com.backbase.stream.config.BootstrapConfigurationProperties;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.Assert;

@SpringBootTest
@WireMockTest(httpPort = 10000)
@ActiveProfiles({"it", "moustache-bank", "moustache-bank-subsidiaries"})
public class SetupLegalEntityHierarchyTaskApplicationIT {

    @Autowired
    private BootstrapConfigurationProperties configuration;

    @Test
    void contextLoads() {
        // Triggers the CommandLineRunner which will run the boostrap task to be validated by the WireMock assertions.
        Assert.notEmpty(configuration.getLegalEntity().getSubsidiaries(), "At least one subsidiary should be present.");
        Assert.notNull(configuration.getLegalEntity().getName(), "Legal entity name should be present.");
    }

}
