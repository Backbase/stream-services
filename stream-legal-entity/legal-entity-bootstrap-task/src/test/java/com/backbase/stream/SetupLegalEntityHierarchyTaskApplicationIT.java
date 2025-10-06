package com.backbase.stream;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.backbase.stream.config.BootstrapConfigurationProperties;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.Assert;

//@SpringBootTest
//@ActiveProfiles({"it", "moustache-bank", "moustache-bank-subsidiaries"})
class SetupLegalEntityHierarchyTaskApplicationIT {



}
