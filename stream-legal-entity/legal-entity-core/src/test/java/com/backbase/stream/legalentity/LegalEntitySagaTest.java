package com.backbase.stream.legalentity;

import com.backbase.dbs.accounts.presentation.service.model.ArrangementItem;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityType;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.worker.model.UnitOfWork;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Slf4j
@Ignore
public class LegalEntitySagaTest extends AbstractLegalEntityCoreTests {


    @Test
    public void createOrganisationLegalEntity() throws JsonProcessingException {

        User adminUser = new User().externalId("admin").fullName("Administrator");
        User johnUser = new User().externalId("john").fullName("John");
        User saraUser = new User().externalId("sara").fullName("Sara");
        User backbaseAdminUser = new User().externalId("backbaseAdmin").fullName("Backbase Admin");
        User aspUser = new User().externalId("ASPUser").fullName("ASP User");
        User aspAdminUser = new User().externalId("ASPAdmin").fullName("ASP Admin");

        LegalEntity johnLegalEntity = new LegalEntity();
        johnLegalEntity
            .name("John").externalId("john")
            .legalEntityType(LegalEntityType.CUSTOMER)
            .addAdministratorsItem(johnUser);


        LegalEntity saraLegalEntity = new LegalEntity();
        saraLegalEntity
            .name("Sara")
            .externalId("sara")
            .legalEntityType(LegalEntityType.CUSTOMER).addAdministratorsItem(saraUser);

        LegalEntity asp = new LegalEntity();
        asp.name("ASP").externalId("asp").legalEntityType(LegalEntityType.CUSTOMER)
            .addAdministratorsItem(aspAdminUser);
        LegalEntity cardiff = new LegalEntity();

        cardiff
            .name("Cardiff").externalId("cardiff")
            .legalEntityType(LegalEntityType.CUSTOMER);

        LegalEntity backbase = new LegalEntity();
        backbase.name("Backbase").externalId("backbase")
            .legalEntityType(LegalEntityType.CUSTOMER).addAdministratorsItem(backbaseAdminUser);
        backbase.addSubsidiariesItem(cardiff);

        LegalEntity root = new LegalEntity();
        root.name("Bank").externalId("bank").legalEntityType(LegalEntityType.BANK)
            .addAdministratorsItem(adminUser);
        root.addSubsidiariesItem(johnLegalEntity);
        root.addSubsidiariesItem(saraLegalEntity);
        root.addSubsidiariesItem(backbase);
        root.addSubsidiariesItem(asp);


        LegalEntityTask task = new LegalEntityTask( johnLegalEntity);
        task.setId("root");

        Mono<UnitOfWork<LegalEntityTask>> unitOfWorkMono = unitOfWorkExecutor.executeUnitOfWork(
            UnitOfWork.from("root", Collections.singletonList(task)));
        unitOfWorkMono.block();
    }


    @Test
    public void testCreateLegalEntityAggregateWithProducts() {

        Mono<UnitOfWork<LegalEntityTask>> unitOfWorkMono = unitOfWorkExecutor.executeUnitOfWork(TestUtils.getTestUnitOfWork());

        unitOfWorkMono.block();
        StepVerifier.create(unitOfWorkMono)
            .expectNextCount(1)
            .verifyComplete();

    }


    @Test
    public void testFixedUsers() {
        List<LegalEntityTask> fixedUsers = Stream.generate(() -> TestUtils.createRandomLegalEntity(this, "bart"))
            .limit(1)
            .map(LegalEntityTask::new)
            .collect(Collectors.toList());

        Mono<UnitOfWork<LegalEntityTask>> publisher = unitOfWorkExecutor.executeUnitOfWork(
            UnitOfWork.from("fixed-users", fixedUsers));

        publisher.block();


//        StepVerifier.create(publisher)
//            .expectNextCount(fixedUsers.size())
//            .verifyComplete();
    }


    @Test
    public void testDataGroupsPerServiceAgreements() {
        accessGroupService.getDataGroupItemIdsByExternalServiceAgreementId("sa_john", "ARRANGEMENTS")
            .doOnNext(presentationDataGroupDetails ->
                log.info("{}", presentationDataGroupDetails)
            ).collectList().block();
    }

    @Test
    public void testGetProductsForLegalEntity() {
        Flux<ArrangementItem> john = entitlementsService.getProductsForExternalLegalEntityId("john");
        List<ArrangementItem> products = john.log().collectList()
            .block();
    }

}