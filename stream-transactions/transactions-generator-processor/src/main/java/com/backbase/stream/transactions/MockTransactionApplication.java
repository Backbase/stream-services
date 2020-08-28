package com.backbase.stream.transactions;


import com.backbase.dbs.transaction.presentation.service.model.TransactionItemPost;
import com.backbase.stream.cursor.model.IngestionCursor;
import com.backbase.stream.transaction.generator.TransactionGenerator;
import com.backbase.stream.transaction.generator.configuration.TransactionGeneratorConfiguration;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.handler.annotation.SendTo;

@SpringBootApplication
public class MockTransactionApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockTransactionApplication.class, args);
    }
}

/**
 * Spring Cloud Data Flow Processor for generating random transaction for an ingestion cursor. Only for demo and load
 * testing purposes!
 */
@EnableBinding({Processor.class})
@Slf4j
@AllArgsConstructor
@Import(TransactionGeneratorConfiguration.class)
class MockTransactionProcessor {

    private final TransactionGenerator transactionGenerator;

    /**
     * Create Random transactions for ingestion cursor.
     *
     * @param ingestionCursor Ingestion Cursor
     * @return List of random Transactions
     */
    @StreamListener(Processor.INPUT)
    @SendTo(Processor.OUTPUT)
    public List<TransactionItemPost> retrieveTransactions(IngestionCursor ingestionCursor) {
        return transactionGenerator.generate(ingestionCursor, 1, 10);
    }


}
