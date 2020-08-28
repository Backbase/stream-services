package com.backbase.stream.legalentity;

import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.legalentity.generator.LegalEntityGenerator;
import com.backbase.stream.legalentity.generator.configuration.LegalEntityGeneratorConfigurationProperties;
import com.backbase.stream.legalentity.generator.utils.DefaultGeneratorProductKindOptions;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.product.generator.ProductGenerator;
import com.backbase.stream.productcatalog.model.ProductCatalog;
import com.backbase.stream.worker.model.UnitOfWork;
import java.time.Duration;
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
public class LegalEntityUnitOfWorkTest extends AbstractLegalEntityCoreTests {

    @Test
    public void testCreateUnitOfWork() {
        UnitOfWork<LegalEntityTask> unitOfWork = TestUtils.getTestUnitOfWork();

        Mono<UnitOfWork<LegalEntityTask>> register = unitOfWorkExecutor.register(unitOfWork)
            .log();
        StepVerifier.create(register)
            .expectNextCount(1)
            .verifyComplete();

        Mono<UnitOfWork<LegalEntityTask>> retrieve = unitOfWorkRepository.findById(unitOfWork.getUnitOfOWorkId())
            .log();

        StepVerifier.create(retrieve)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    public void testCreateLargeUnitOfWOrk() {
        LegalEntityGeneratorConfigurationProperties options = new LegalEntityGeneratorConfigurationProperties();
        ProductGenerator productGenerator = new ProductGenerator(DefaultGeneratorProductKindOptions.getProductGeneratorConfigurationProperties());
        ProductCatalog productCatalog = productCatalogService.getProductCatalog().block();

        legalEntityGenerator = new LegalEntityGenerator(options, productGenerator);

        List<LegalEntity> legalEntityStream = Stream.generate(() -> legalEntityGenerator.generate(productCatalog))
            .limit(10).collect(Collectors.toList());

        TestUtils.writeToYaml(legalEntityStream);

        Flux<UnitOfWork<LegalEntityTask>> unitOfWorkFlux = Flux.fromIterable(legalEntityStream).bufferTimeout(10, Duration.ofMillis(1000))
            .map(this::createUnitOfWork)
            .flatMap(unitOfWorkExecutor::register);

        unitOfWorkFlux.doOnNext(actual -> log.info("Unit Of Work: {}", actual.getUnitOfOWorkId()))
            .collectList()
            .doOnNext(list -> log.info("Should have 10: {}", list.size()))
            .block();
    }

    private UnitOfWork<LegalEntityTask> createUnitOfWork(List<LegalEntity> legalEntities) {
        List<LegalEntityTask> tasks = legalEntities.stream()
            .map(LegalEntityTask::new)
            .collect(Collectors.toList());
        return UnitOfWork.from("http-" + System.currentTimeMillis(), tasks);
    }

    @Test
    public void testExecuteUnitOfWork() {
        unitOfWorkExecutor.executeUnitOfWork(TestUtils.getTestUnitOfWork())
            .block();
    }


    @Test
    public void testSelectUnitOfWork() throws InterruptedException {

        unitOfWorkRepository.deleteAll()
            .doOnError(throwable -> log.error("error; {}", throwable.getMessage(), throwable))
            .doOnNext(v -> log.info("Rest Unit Of Work"))
            .block();

        unitOfWorkExecutor.register(TestUtils.getTestUnitOfWork()).block();

        unitOfWorkExecutor.getScheduler()
            .blockLast();


    }


}