package com.backbase.stream;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@WireMockTest(httpPort = 10000)
@ActiveProfiles({"it", "moustache-bank"})
public class SetupLegalEntityHierarchyTaskApplicationIT {

    @Test
    void contextLoads() {

    }

}
