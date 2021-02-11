package com.backbase.stream.audit;

import com.backbase.dbs.audit.api.service.v2.model.AuditMessage;
import com.backbase.dbs.audit.api.service.v2.model.AuditMessagesPostRequest;
import com.backbase.dbs.audit.api.service.v2.model.Status;
import com.backbase.stream.AbstractServiceIntegrationTests;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.javafaker.Faker;
import java.text.DateFormat;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;


@Slf4j
public class AuditUnitOfWorkExecutorIT extends AbstractServiceIntegrationTests {

    private Faker faker = Faker.instance();

    @Test
    public void prepareAuditMessages() throws JsonProcessingException {

        List<AuditMessage> collect = Stream.generate(this::randomAuditMessage)
            .limit(86).collect(Collectors.toList());
        DateFormat dateFormat = new StdDateFormat();
        dateFormat.setTimeZone(TimeZone.getDefault());
        AuditMessagesPostRequest requestBody = new AuditMessagesPostRequest();
        requestBody.setAuditMessages(collect);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setDateFormat(dateFormat);



        log.info("Random Audit Messages: \n{}", objectMapper.writeValueAsString(requestBody));





    }


    public AuditMessage randomAuditMessage() {
        return new AuditMessage()
            .messageSetId(UUID.randomUUID().toString())
            .eventAction("Stream")
            .eventCategory(faker.app().name())
            .objectType("Audit")
            .eventAction("Generate")
            .status(faker.bool().bool() ?  Status.SUCCESSFUL : Status.FAILED)
            .username(faker.starTrek().character())
            .userId(faker.idNumber().ssnValid())
            .timestamp(OffsetDateTime.now())
            .eventDescription(faker.chuckNorris().fact())
            .legalEntityId(faker.idNumber().ssnValid())
            .serviceAgreementId(faker.idNumber().ssnValid())
            .ipAddress("127.0.0.1")
            .userAgent("Mozilla/5.0 (Windows NT 6.3; Trident/7.0; rv:11.0) like Gecko");




    }

}
