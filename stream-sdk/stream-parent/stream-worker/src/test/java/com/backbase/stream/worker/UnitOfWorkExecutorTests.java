package com.backbase.stream.worker;


import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;
import com.backbase.stream.worker.model.StreamTask;
import com.backbase.stream.worker.model.UnitOfWork;
import com.backbase.stream.worker.repository.impl.InMemoryReactiveUnitOfWorkRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.publisher.Mono;

@Slf4j
@Ignore
public class UnitOfWorkExecutorTests {


    @Test
    public void testConcurrency() {


        StreamWorkerConfiguration streamWorkerConfiguration = new StreamWorkerConfiguration();
        streamWorkerConfiguration.setTaskExecutors(2);

        RandomUnitOfWorkExecutor randomUnitOfWorkExecutor = new RandomUnitOfWorkExecutor(streamWorkerConfiguration);

        List<RandomTask> tasks = IntStream.range(0, 10).mapToObj(i -> {
            String taskId = "Unit Of Work: " + i + " Task: " + (i);
            int timeToExecute = getRandomNumberInRange(5000, 10000);
            return new RandomTask(taskId, timeToExecute);
        }).collect(Collectors.toList());


        UnitOfWork<RandomTask> unitOfWork = UnitOfWork.from("random", tasks);

        randomUnitOfWorkExecutor.executeUnitOfWork(unitOfWork).block();



    }

    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }


    static class RandomTask extends StreamTask {

        final long timeToExecute;

        public RandomTask(String id, long timeToExecute) {
            super(id);
            this.timeToExecute = timeToExecute;
        }

        @Override
        public String getName() {
            return "random";
        }
    }


    static class RandomUnitOfWorkExecutor extends UnitOfWorkExecutor<RandomTask> {

        public RandomUnitOfWorkExecutor(StreamWorkerConfiguration streamWorkerConfiguration) {
            super(new InMemoryReactiveUnitOfWorkRepository<>(), new RandomTaskExecutor(), streamWorkerConfiguration);
        }

    }


    static class RandomTaskExecutor implements StreamTaskExecutor<RandomTask> {

        @Override
        public Mono<RandomTask> executeTask(RandomTask streamTask) {
            return doSomeThing(streamTask);
//                .doOnNext(t -> log.info("Finished task: {}", t.getId()));
        }

        private Mono<RandomTask> doSomeThing(RandomTask streamTask) {

            return Mono.fromRunnable(() -> {
                try (ProgressBar pb = new ProgressBarBuilder()
                    .setInitialMax(streamTask.timeToExecute)
                    .setTaskName(streamTask.getId())
//                .setConsumer(new DelegatingProgressBarConsumer(log::info))
                    .build()) {
                    try {
                        long numberOfSleeps = streamTask.timeToExecute / 100;
                        for (int i = 0; i < numberOfSleeps; i++) {
                            Thread.sleep(100);
                            pb.stepBy(100);
                        }
                        pb.stepTo(streamTask.timeToExecute);
                        pb.setExtraMessage(Thread.currentThread().getName());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });


        }

        @Override
        public Mono<RandomTask> rollBack(RandomTask streamTask) {
            return null;
        }
    }


}
