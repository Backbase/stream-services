package com.backbase.stream.paymentorder;

import static com.backbase.dbs.paymentorder.api.service.v3.model.Status.ACCEPTED;
import static com.backbase.dbs.paymentorder.api.service.v3.model.Status.CANCELLATION_PENDING;
import static com.backbase.dbs.paymentorder.api.service.v3.model.Status.CANCELLED;
import static com.backbase.dbs.paymentorder.api.service.v3.model.Status.PROCESSED;
import static com.backbase.dbs.paymentorder.api.service.v3.model.Status.READY;
import static com.backbase.dbs.paymentorder.api.service.v3.model.Status.REJECTED;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static reactor.core.publisher.Flux.defer;
import static reactor.core.publisher.Flux.empty;
import static reactor.util.retry.Retry.fixedDelay;

import com.backbase.dbs.arrangement.api.service.v3.ArrangementsApi;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementItem;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementSearchesListResponse;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementsSearchesPostRequest;
import com.backbase.dbs.paymentorder.api.service.v3.model.AccessFilter;
import com.backbase.dbs.paymentorder.api.service.v3.model.Status;
import com.backbase.stream.config.PaymentOrderTypeConfiguration;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.backbase.dbs.paymentorder.api.service.v3.PaymentOrdersApi;
import com.backbase.dbs.paymentorder.api.service.v3.model.GetPaymentOrderResponse;
import com.backbase.dbs.paymentorder.api.service.v3.model.PaymentOrderPostFilterRequest;
import com.backbase.dbs.paymentorder.api.service.v3.model.PaymentOrderPostFilterResponse;
import com.backbase.dbs.paymentorder.api.service.v3.model.PaymentOrderPostRequest;
import com.backbase.dbs.paymentorder.api.service.v3.model.SimpleOriginatorAccount;
import com.backbase.stream.config.PaymentOrderWorkerConfigurationProperties;
import com.backbase.stream.mappers.PaymentOrderTypeMapper;
import com.backbase.stream.model.PaymentOrderIngestContext;
import com.backbase.stream.model.request.DeletePaymentOrderIngestRequest;
import com.backbase.stream.model.request.NewPaymentOrderIngestRequest;
import com.backbase.stream.model.request.PaymentOrderIngestRequest;
import com.backbase.stream.model.request.UpdatePaymentOrderIngestRequest;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.UnitOfWorkExecutor;
import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;
import com.backbase.stream.worker.model.UnitOfWork;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class PaymentOrderUnitOfWorkExecutor extends UnitOfWorkExecutor<PaymentOrderTask> {

    private static final List<Status> FILTER = List.of(READY, ACCEPTED, PROCESSED, CANCELLED, REJECTED, CANCELLATION_PENDING);

    private static final int PAGE_SIZE = 1000;

    private final PaymentOrdersApi paymentOrdersApi;
    private final ArrangementsApi arrangementsApi;
    private final PaymentOrderTypeMapper paymentOrderTypeMapper;
    private final PaymentOrderTypeConfiguration paymentOrderTypeConfiguration;

    public PaymentOrderUnitOfWorkExecutor(UnitOfWorkRepository<PaymentOrderTask, String> repository,
            StreamTaskExecutor<PaymentOrderTask> streamTaskExecutor,
            StreamWorkerConfiguration streamWorkerConfiguration,
            PaymentOrdersApi paymentOrdersApi,
            ArrangementsApi arrangementsApi,
            PaymentOrderTypeMapper paymentOrderTypeMapper,
            PaymentOrderTypeConfiguration paymentOrderTypeConfiguration) {
        super(repository, streamTaskExecutor, streamWorkerConfiguration);
        this.paymentOrdersApi = paymentOrdersApi;
        this.arrangementsApi = arrangementsApi;
        this.paymentOrderTypeMapper = paymentOrderTypeMapper;
        this.paymentOrderTypeConfiguration = paymentOrderTypeConfiguration;
    }

    public Flux<UnitOfWork<PaymentOrderTask>> prepareUnitOfWork(List<PaymentOrderIngestRequest> items) {

        Stream<UnitOfWork<PaymentOrderTask>> unitOfWorkStream;
        String unitOfOWorkId = "payment-orders-mixed-" + System.currentTimeMillis();
        PaymentOrderTask task = new PaymentOrderTask(unitOfOWorkId, items);
        unitOfWorkStream = Stream.of(UnitOfWork.from(unitOfOWorkId, task));
        return Flux.fromStream(unitOfWorkStream);
    }

    public Flux<UnitOfWork<PaymentOrderTask>> prepareUnitOfWork(Flux<PaymentOrderPostRequest> items) {
        return items.collectList()
                .map(this::createPaymentOrderIngestContext)
                .flatMap(this::addArrangementIdMap)
                .flatMap(this::getPersistedScheduledTransfers)
                .flatMapMany(this::getPaymentOrderIngestRequest)
                .bufferTimeout(streamWorkerConfiguration.getBufferSize(), streamWorkerConfiguration.getBufferMaxTime())
                .flatMap(this::prepareUnitOfWork);
    }

    private PaymentOrderIngestContext createPaymentOrderIngestContext(List<PaymentOrderPostRequest> paymentOrderPostRequests) {
        PaymentOrderIngestContext paymentOrderIngestContext = new PaymentOrderIngestContext();
        paymentOrderIngestContext.corePaymentOrder(paymentOrderPostRequests == null ? emptyList() : paymentOrderPostRequests);
        paymentOrderIngestContext.internalUserId(getInternalUserId(paymentOrderPostRequests));
        return paymentOrderIngestContext;
    }

    /**
     * Gets the list of payments that are persisted in DBS for a specific user.
     * The transfers have been divided by destination product type.
     *
     * @param paymentOrderIngestContext Holds all the Ingestion details.
     * @return A Mono of List of GetPaymentOrderResponse.
     */
    private @NotNull @Valid Mono<PaymentOrderIngestContext> getPersistedScheduledTransfers(PaymentOrderIngestContext paymentOrderIngestContext) {

        List<GetPaymentOrderResponse> listOfPayments = new ArrayList<>();

        return getPayments(paymentOrderIngestContext.arrangementIds())
                .map(response -> {
                    listOfPayments.addAll(response.getPaymentOrders());
                    return listOfPayments;
                })
                .map(getPaymentOrderResponses -> paymentOrderIngestContext.existingPaymentOrder(getPaymentOrderResponses))
                .doOnSuccess(result ->
                        log.debug("Successfully fetched dbs scheduled payment orders"));
    }

    private @NotNull @Valid Mono<PaymentOrderIngestContext> addArrangementIdMap(PaymentOrderIngestContext paymentOrderIngestContext) {

        return Flux.fromIterable(paymentOrderIngestContext.corePaymentOrder())
                .flatMap(this::getArrangement)
                .distinct()
                .collectList()
                .map(accountInternalIdGetResponseBody -> paymentOrderIngestContext.arrangementIds(accountInternalIdGetResponseBody));
    }

    private Mono<ArrangementSearchesListResponse> getArrangement(PaymentOrderPostRequest paymentOrderPostRequest) {
        SimpleOriginatorAccount originatorAccount = paymentOrderPostRequest.getOriginatorAccount();
        if (originatorAccount == null) {
            return Mono.empty();
        }
        return arrangementsApi.postSearchArrangements(new ArrangementsSearchesPostRequest()
            .externalArrangementIds(Collections.singleton(originatorAccount.getExternalArrangementId())));
    }

    /**
     * Calls the payment order service to retrieve existing payments.
     *
     * @param arrangementIds   Check for all the arrangements that belong to this PMT.
     * @return A Mono with the response from the service api.
     */
    private Mono<PaymentOrderPostFilterResponse> getPayments(List<ArrangementSearchesListResponse> arrangementIds) {

        if (isEmptyArrangmetIds(arrangementIds)) {
            return Mono.just(new PaymentOrderPostFilterResponse().paymentOrders(emptyList()).totalElements(new BigDecimal(0)));
        }
        List<String> allArrangementIds = arrangementIds
                .stream().flatMap(accountArrangementItems ->
                        accountArrangementItems.getArrangementElements().stream())
                .map(ArrangementItem::getId)
                .collect(Collectors.toList());

        return pullFromDBS(allArrangementIds).map(result -> {
            final var response = new PaymentOrderPostFilterResponse();
            response.setPaymentOrders(result);
            response.setTotalElements(new BigDecimal(result.size()));
            return response;
        });
    }

    private Flux<PaymentOrderIngestRequest> getPaymentOrderIngestRequest(PaymentOrderIngestContext paymentOrderIngestContext) {

        List<PaymentOrderIngestRequest> paymentOrderIngestRequests = new ArrayList<>();
        final var userId = paymentOrderIngestContext.internalUserId();
        if (isEmptyUserId(userId)) {
            return Flux.fromIterable(paymentOrderIngestRequests);
        }
        final List<PaymentOrderPostRequest> orders = paymentOrderIngestContext.corePaymentOrder() == null ? new ArrayList<>() : paymentOrderIngestContext.corePaymentOrder();

        // list of all the bank ref ids in core
        List<String> coreBankRefIds = new ArrayList<>();
        for (PaymentOrderPostRequest coreBankRefId : orders) {
            coreBankRefIds.add(coreBankRefId.getBankReferenceId());
        }

        // list of all the bank ref ids in DBS (existing)
        List<String> existingBankRefIds = new ArrayList<>();
        for (GetPaymentOrderResponse existingBankRefId : paymentOrderIngestContext.existingPaymentOrder() ) {
            existingBankRefIds.add(existingBankRefId.getBankReferenceId());
        }

        final List<GetPaymentOrderResponse> existing = paymentOrderIngestContext.existingPaymentOrder() == null ? new ArrayList<>() : paymentOrderIngestContext.existingPaymentOrder();

        // build new payment list (Bank ref is in core, but not in DBS)
        orders.forEach(corePaymentOrder -> {
            if(!existingBankRefIds.contains(corePaymentOrder.getBankReferenceId())) {
                SimpleOriginatorAccount simpleOriginatorAccount = corePaymentOrder.getOriginatorAccount();
                if (simpleOriginatorAccount == null) {
                    return;
                }
                ArrangementItem arrangementItem = getInternalArrangementId(paymentOrderIngestContext.arrangementIds(),
                    simpleOriginatorAccount.getExternalArrangementId());
                if (arrangementItem != null) {
                    simpleOriginatorAccount.setArrangementId(arrangementItem.getId());
                }
                paymentOrderIngestRequests.add(new NewPaymentOrderIngestRequest(corePaymentOrder));
            }
        });

        // build update payment list (Bank ref is in core and DBS)
        orders.forEach(corePaymentOrder -> {
            if(existingBankRefIds.contains(corePaymentOrder.getBankReferenceId())) {
                UpdatePaymentOrderIngestRequest updatePaymentOrderIngestRequest = new UpdatePaymentOrderIngestRequest(paymentOrderTypeMapper.mapPaymentOrderPostRequest(corePaymentOrder));
                paymentOrderIngestRequests.add(updatePaymentOrderIngestRequest);
            }
        });

        // build delete payment list (Bank ref is in DBS, but not in core)
        buildDeletePaymentList(existing, coreBankRefIds, paymentOrderIngestRequests);

        return Flux.fromIterable(paymentOrderIngestRequests);
    }

    private void buildDeletePaymentList(List<GetPaymentOrderResponse> existing, List<String> coreBankRefIds, List<PaymentOrderIngestRequest> paymentOrderIngestRequests) {
        if (((PaymentOrderWorkerConfigurationProperties) streamWorkerConfiguration).isDeletePaymentOrder()) {
            existing.forEach(existingPaymentOrder -> {
                if(!coreBankRefIds.contains(existingPaymentOrder.getBankReferenceId())) {
                    paymentOrderIngestRequests.add(new DeletePaymentOrderIngestRequest(existingPaymentOrder.getId(), existingPaymentOrder.getBankReferenceId()));
                }
            });
        }
    }

    private ArrangementItem getInternalArrangementId(List<ArrangementSearchesListResponse> accountArrangementItemsList, String externalArrangementId) {
        return accountArrangementItemsList.stream()
                .flatMap(a -> a.getArrangementElements().stream())
                .filter(b -> Objects.nonNull(b.getExternalArrangementId()))
                .filter(b -> b.getExternalArrangementId().equalsIgnoreCase(externalArrangementId))
                .findFirst()
                .orElse(null);
    }

    private record DBSPaymentOrderPageResult(int next, int total, List<GetPaymentOrderResponse> requests) {

    }

    private Mono<List<GetPaymentOrderResponse>> pullFromDBS(final @NotNull List<String> arrangementIds) {
        return defer(() -> retrieveNextPage(0, arrangementIds)
                .expand(page -> {
                    // If there are no more pages, return an empty flux.
                    if (page.next >= page.total || page.requests.isEmpty()) {
                        return empty();
                    } else {
                        return retrieveNextPage(page.next, arrangementIds);
                    }
                }))
                .collectList()
                .map(pages -> pages.stream().flatMap(page -> page.requests.stream()).toList());
    }

    private Mono<DBSPaymentOrderPageResult> retrieveNextPage(int currentCount, final @NotNull List<String> arrangementIds) {
        List<String> paymentTypes = paymentOrderTypeConfiguration.getTypes();
        var paymentOrderPostFilterRequest = new PaymentOrderPostFilterRequest();
        List<AccessFilter> accessFilters = paymentTypes.stream()
                .map(paymentType -> new AccessFilter()
                        .paymentType(paymentType)
                        .arrangementIds(arrangementIds))
                .collect(Collectors.toList());
        paymentOrderPostFilterRequest.setAccessFilters(accessFilters);
        paymentOrderPostFilterRequest.setStatuses(FILTER);

        return paymentOrdersApi.postFilterPaymentOrders(null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, currentCount / PAGE_SIZE, PAGE_SIZE, null,
                        null, paymentOrderPostFilterRequest)
                .retryWhen(fixedDelay(3, Duration.of(2000, MILLIS)).filter(
                        t -> t instanceof WebClientRequestException
                                || t instanceof WebClientResponseException.ServiceUnavailable))
                .map(resp -> {
                    // getPaymentOrders() is marked with @NotNull, dunno why still checking its nullability?
                    final List<GetPaymentOrderResponse> results = ofNullable(resp.getPaymentOrders()).orElse(emptyList());
                    // getTotalElements() is marked with @NotNull, dunno why still checking its nullability?
                    final var total = ofNullable(resp.getTotalElements()).orElse(new BigDecimal(0)).intValue();
                    return new DBSPaymentOrderPageResult(currentCount + results.size(), total, results);
                });
    }

    private boolean isEmptyArrangmetIds(List<ArrangementSearchesListResponse> arrangementItems) {
        return arrangementItems == null || arrangementItems.isEmpty();
    }

    private boolean isEmptyUserId(String userId) {
        return userId == null || userId.isBlank();
    }

    private String getInternalUserId(List<PaymentOrderPostRequest> paymentOrderPostRequests) {
        if (paymentOrderPostRequests == null || paymentOrderPostRequests.isEmpty()) {
            return null;
        } else {
            return paymentOrderPostRequests.getFirst().getInternalUserId();
        }
    }
}
