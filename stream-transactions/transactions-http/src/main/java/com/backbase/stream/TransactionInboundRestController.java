package com.backbase.stream;

import com.backbase.stream.mapper.TransactionItemMapper;
import com.backbase.stream.transaction.TransactionTask;
import com.backbase.stream.transaction.TransactionUnitOfWorkExecutor;
import com.backbase.stream.transaction.TransactionsQuery;
import com.backbase.stream.transaction.inbound.api.TransactionsApi;
import com.backbase.stream.transaction.inbound.model.ArrangementItem;
import com.backbase.stream.transaction.inbound.model.TransactionPostResponse;
import com.backbase.stream.transaction.inbound.model.TransactionsDeleteRequestBody;
import com.backbase.stream.transaction.inbound.model.TransactionsGet;
import com.backbase.stream.transaction.inbound.model.TransactionsPatchRequestBody;
import com.backbase.stream.transaction.inbound.model.TransactionsPostTransactionPost;
import com.backbase.stream.worker.model.UnitOfWork;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@AllArgsConstructor
public class TransactionInboundRestController implements TransactionsApi {

    private final TransactionService transactionService;
    private final TransactionUnitOfWorkExecutor transactionUnitOfWorkExecutor;

    private final TransactionItemMapper mapper = Mappers.getMapper(TransactionItemMapper.class);

    @Override
    public Mono<ResponseEntity<TransactionsGet>> getTransactions(@Valid BigDecimal amountGreaterThan,
        @Valid BigDecimal amountLessThan, @Valid String bookingDateGreaterThan, @Valid String bookingDateLessThan,
        @Valid String type, @Valid List<String> types, @Valid String description, @Valid String reference,
        @Valid String typeGroup, @Valid List<String> typeGroups,
        @Valid String counterPartyName, @Valid String counterPartyAccountNumber, @Valid String creditDebitIndicator,
        @Valid String category, @Valid List<String> categories, @Valid String billingStatus, @Valid String currency,
        @Valid Integer notes, @Valid String id, @Valid String arrangementId, @Valid List<String> arrangementsIds,
        @Valid Long fromCheckSerialNumber, @Valid Long toCheckSerialNumber,
        @Valid List<Long> checkSerialNumbers, @Valid String query, @Valid Integer from, @Valid String cursor,
        @Valid Integer size, @Valid String orderBy, @Valid String direction, @Valid String secDirection,
        ServerWebExchange exchange) {

        TransactionsQuery transactionsQuery = new TransactionsQuery(amountGreaterThan, amountLessThan,
            bookingDateGreaterThan, bookingDateLessThan, types, description, reference, typeGroups,
            counterPartyName, counterPartyAccountNumber, creditDebitIndicator, categories, billingStatus, null,
            currency, notes, id, null, arrangementId, arrangementsIds, fromCheckSerialNumber,
            toCheckSerialNumber, checkSerialNumbers, query, from, cursor, size, orderBy, direction, secDirection);
        return transactionService.getTransactions(transactionsQuery)
            .map(mapper::toInbound)
            .collectList()
            .map(transactionItems -> new TransactionsGet()
                .transactionItems(transactionItems)
                .totalElements(BigDecimal.valueOf(transactionItems.size())))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> patchTransactions(
        @Valid Flux<TransactionsPatchRequestBody> transactionsPatchRequestBody, ServerWebExchange exchange) {
        return transactionService.patchTransactions(transactionsPatchRequestBody.map(mapper::toPresentation))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> postDelete(
        @Valid Flux<TransactionsDeleteRequestBody> transactionsDeleteRequestBody, ServerWebExchange exchange) {
        return transactionService.deleteTransactions(transactionsDeleteRequestBody.map(mapper::toPresentation))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> postRefresh(@Valid Flux<ArrangementItem> arrangementItem,
        ServerWebExchange exchange) {
        return transactionService.postRefresh(arrangementItem.map(mapper::toPresentation))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<TransactionPostResponse>>> postTransactions(
        @Valid Flux<TransactionsPostTransactionPost> transactionsPostTransactionPost, ServerWebExchange exchange) {
        Flux<UnitOfWork<TransactionTask>> unitOfWorkFlux = transactionUnitOfWorkExecutor
            .prepareUnitOfWork(transactionsPostTransactionPost.map(mapper::toPresentation));

        Flux<TransactionPostResponse> transactionPostResponseFlux = unitOfWorkFlux
            .flatMap(transactionUnitOfWorkExecutor::executeUnitOfWork)
            .flatMap(unitOfWork -> Flux.fromStream(unitOfWork.getStreamTasks().stream()
                .map(TransactionTask::getResponse)
                .flatMap(Collection::stream)))
            .map(mapper::toInbound);

        return Mono.just(ResponseEntity.ok(transactionPostResponseFlux));
    }
}
