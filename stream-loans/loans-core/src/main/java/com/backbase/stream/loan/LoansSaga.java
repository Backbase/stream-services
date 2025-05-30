package com.backbase.stream.loan;

import com.backbase.loan.inbound.api.service.v2.LoansApi;
import com.backbase.loan.inbound.api.service.v2.model.BatchUpsertLoans;
import com.backbase.loan.inbound.api.service.v2.model.InboundIntegrationArrangementAttributes;
import com.backbase.loan.inbound.api.service.v2.model.InboundIntegrationLoan;
import com.backbase.stream.worker.StreamTaskExecutor;
import io.micrometer.tracing.annotation.SpanTag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class LoansSaga implements StreamTaskExecutor<LoansTask> {

    private final LoanMapper loanMapper = Mappers.getMapper(LoanMapper.class);

    private final LoansApi loansApi;

    @Override
    public Mono<LoansTask> executeTask(@SpanTag(value = "streamTask") LoansTask streamTask) {
        List<InboundIntegrationLoan> inboundLoans =
            streamTask.getData().stream()
                .map(loanMapper::map)
                .toList();
        List<String> externalIds = inboundLoans.stream()
            .map(InboundIntegrationLoan::getArrangementAttributes)
            .map(InboundIntegrationArrangementAttributes::getExternalId)
            .toList();
        log.info("Started ingestion of loans with external arrangement ids {}", externalIds);
        return Flux.fromIterable(inboundLoans)
            .buffer(50)
            .concatMap(batch ->
                loansApi.postBatchUpsertLoans(new BatchUpsertLoans().loans(batch))
                    .doOnNext(upsertLoanResponse -> {
                        streamTask.setResponse(upsertLoanResponse);
                        streamTask.info("loan", "upsert", "success", upsertLoanResponse.getResourceId(),
                            upsertLoanResponse.getArrangementId(), "upsert is successful");
                    })
                    .collectList())
            .collectList()
            .thenReturn(streamTask);
    }

    @Override
    public Mono<LoansTask> rollBack(LoansTask streamTask) {
        return null;
    }
}
