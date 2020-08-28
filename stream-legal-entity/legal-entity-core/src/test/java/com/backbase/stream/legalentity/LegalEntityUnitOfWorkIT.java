package com.backbase.stream.legalentity;

import com.backbase.stream.LegalEntityUnitOfWorkExecutor;
import com.backbase.stream.configuration.LegalEntitySagaConfiguration;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = LegalEntityUnitOfWorkIT.SpringBootTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@ActiveProfiles("test")
@Slf4j
@Ignore
public class LegalEntityUnitOfWorkIT {


    @Autowired
    LegalEntityUnitOfWorkExecutor unitOfWorkExecutor;

    @Test
    public void test() throws InterruptedException {
        unitOfWorkExecutor.register(TestUtils.getTestUnitOfWork()).block();
        unitOfWorkExecutor.getScheduler().blockLast(Duration.ofMinutes(5));
    }


    @SpringBootApplication
    @Import(LegalEntitySagaConfiguration.class)
    public static class SpringBootTestApplication {

    }


}
