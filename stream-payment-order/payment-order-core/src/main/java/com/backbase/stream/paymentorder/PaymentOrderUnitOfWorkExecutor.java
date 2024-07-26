package com.backbase.stream.paymentorder;

import static com.backbase.dbs.paymentorder.api.service.v2.model.Status.ACCEPTED;
import static com.backbase.dbs.paymentorder.api.service.v2.model.Status.CANCELLATION_PENDING;
import static com.backbase.dbs.paymentorder.api.service.v2.model.Status.CANCELLED;
import static com.backbase.dbs.paymentorder.api.service.v2.model.Status.PROCESSED;
import static com.backbase.dbs.paymentorder.api.service.v2.model.Status.READY;
import static com.backbase.dbs.paymentorder.api.service.v2.model.Status.REJECTED;

import com.backbase.dbs.arrangement.api.service.v2.ArrangementsApi;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItem;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItems;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementsFilter;
import com.backbase.dbs.paymentorder.api.service.v2.model.AccessFilter;
import com.backbase.stream.config.PaymentOrderTypeConfiguration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.backbase.dbs.paymentorder.api.service.v2.PaymentOrdersApi;
import com.backbase.dbs.paymentorder.api.service.v2.model.GetPaymentOrderResponse;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostFilterRequest;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostFilterResponse;
import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
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

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class PaymentOrderUnitOfWorkExecutor extends UnitOfWorkExecutor<PaymentOrderTask> {

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
                .map(paymentOrderPostRequests -> this.createPaymentOrderIngestContext(paymentOrderPostRequests))
                .flatMap(this::addArrangementIdMap)
                .flatMap(this::getPersistedScheduledTransfers)
                .flatMapMany(this::getPaymentOrderIngestRequest)
                .bufferTimeout(streamWorkerConfiguration.getBufferSize(), streamWorkerConfiguration.getBufferMaxTime())
                .flatMap(this::prepareUnitOfWork);
    }

    private PaymentOrderIngestContext createPaymentOrderIngestContext(List<PaymentOrderPostRequest> paymentOrderPostRequests) {
        PaymentOrderIngestContext paymentOrderIngestContext = new PaymentOrderIngestContext();
        paymentOrderIngestContext.corePaymentOrder(paymentOrderPostRequests);
        paymentOrderIngestContext.internalUserId(paymentOrderPostRequests.get(0).getInternalUserId());
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

    private Mono<AccountArrangementItems> getArrangement(PaymentOrderPostRequest paymentOrderPostRequest) {
        AccountArrangementsFilter accountArrangementsFilter = new AccountArrangementsFilter()
                .externalArrangementIds(Collections.singletonList(paymentOrderPostRequest.getOriginatorAccount().getExternalArrangementId()));
        return arrangementsApi.postFilter(accountArrangementsFilter);
    }

    /**
     * Calls the payment order service to retrieve existing payments.
     *
     * @param arrangementIds   Check for all the arrangements that belong to this PMT.
     * @return A Mono with the response from the service api.
     */
    private Mono<PaymentOrderPostFilterResponse> getPayments(List<AccountArrangementItems> arrangementIds) {

        List<String> allArrangementIds = arrangementIds
                .stream().flatMap(accountArrangementItems ->
                        accountArrangementItems.getArrangementElements().stream())
                .map(AccountArrangementItem::getId)
                .collect(Collectors.toList());

        List<String> paymentTypes = paymentOrderTypeConfiguration.getTypes();
        var paymentOrderPostFilterRequest = new PaymentOrderPostFilterRequest();

        List<AccessFilter> accessFilters = paymentTypes.stream()
                .map(paymentType -> new AccessFilter()
                        .paymentType(paymentType)
                        .arrangementIds(allArrangementIds))
                .collect(Collectors.toList());

        paymentOrderPostFilterRequest.setAccessFilters(accessFilters);
        paymentOrderPostFilterRequest.setStatuses(
                List.of(READY, ACCEPTED, PROCESSED, CANCELLED, REJECTED, CANCELLATION_PENDING));
        
        log.debug("request POJO: {}", paymentOrderPostFilterRequest);

        return paymentOrdersApi.postFilterPaymentOrders(
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, Integer.MAX_VALUE,
                null, null, paymentOrderPostFilterRequest);
    }

    private Flux<PaymentOrderIngestRequest> getPaymentOrderIngestRequest(PaymentOrderIngestContext paymentOrderIngestContext) {

        List<PaymentOrderIngestRequest> paymentOrderIngestRequests = new ArrayList<>();

        // list of all the bank ref ids in core
        List<String> coreBankRefIds = new ArrayList<>();
        for (PaymentOrderPostRequest coreBankRefId : paymentOrderIngestContext.corePaymentOrder() ) {
            coreBankRefIds.add(coreBankRefId.getBankReferenceId());
        }

        // list of all the bank ref ids in DBS (existing)
        List<String> existingBankRefIds = new ArrayList<>();
        for (GetPaymentOrderResponse existingBankRefId : paymentOrderIngestContext.existingPaymentOrder() ) {
            existingBankRefIds.add(existingBankRefId.getBankReferenceId());
        }

        // build new payment list (Bank ref is in core, but not in DBS)
        paymentOrderIngestContext.corePaymentOrder().forEach(corePaymentOrder -> {
            if(!existingBankRefIds.contains(corePaymentOrder.getBankReferenceId())) {
                AccountArrangementItem accountArrangementItem = getInternalArrangementId(paymentOrderIngestContext.arrangementIds(),
                        corePaymentOrder.getOriginatorAccount().getExternalArrangementId());
                if (accountArrangementItem != null) {
                    corePaymentOrder.getOriginatorAccount().setArrangementId(accountArrangementItem.getId());
                }
                paymentOrderIngestRequests.add(new NewPaymentOrderIngestRequest(corePaymentOrder));
            }
        });

        // build update payment list (Bank ref is in core and DBS)
        paymentOrderIngestContext.corePaymentOrder().forEach(corePaymentOrder -> {
            if(existingBankRefIds.contains(corePaymentOrder.getBankReferenceId())) {
                UpdatePaymentOrderIngestRequest updatePaymentOrderIngestRequest = new UpdatePaymentOrderIngestRequest(paymentOrderTypeMapper.mapPaymentOrderPostRequest(corePaymentOrder));
                paymentOrderIngestRequests.add(updatePaymentOrderIngestRequest);
            }
        });

        // build delete payment list (Bank ref is in DBS, but not in core)
        if (((PaymentOrderWorkerConfigurationProperties) streamWorkerConfiguration).isDeletePaymentOrder()) {
            paymentOrderIngestContext.existingPaymentOrder().forEach(existingPaymentOrder -> {
                if(!coreBankRefIds.contains(existingPaymentOrder.getBankReferenceId())) {
                    paymentOrderIngestRequests.add(new DeletePaymentOrderIngestRequest(existingPaymentOrder.getId(), existingPaymentOrder.getBankReferenceId()));
                }
            });
        }

        return Flux.fromIterable(paymentOrderIngestRequests);
    }

    private AccountArrangementItem getInternalArrangementId(List<AccountArrangementItems> accountArrangementItemsList, String externalArrangementId) {

        return accountArrangementItemsList.stream()
                .flatMap(a -> a.getArrangementElements().stream())
                .filter(b -> b.getExternalArrangementId().equalsIgnoreCase(externalArrangementId))
                .findFirst()
                .orElse(null);
    }
}
