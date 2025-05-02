package com.backbase.stream.product;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.backbase.dbs.accesscontrol.api.service.v3.model.FunctionGroupItem;
import com.backbase.dbs.arrangement.api.integration.v3.model.PostArrangement;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementItem;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementPutItem;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.CreditCard;
import com.backbase.stream.legalentity.model.CurrentAccount;
import com.backbase.stream.legalentity.model.CustomDataGroupItem;
import com.backbase.stream.legalentity.model.DebitCard;
import com.backbase.stream.legalentity.model.IdentityUserLinkStrategy;
import com.backbase.stream.legalentity.model.InvestmentAccount;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.LegalEntityReference;
import com.backbase.stream.legalentity.model.Loan;
import com.backbase.stream.legalentity.model.Product;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.SavingsAccount;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.TermDeposit;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.loan.LoansSaga;
import com.backbase.stream.product.configuration.ProductIngestionSagaConfigurationProperties;
import com.backbase.stream.product.exception.ArrangementCreationException;
import com.backbase.stream.product.exception.ArrangementUpdateException;
import com.backbase.stream.product.mapping.ProductMapper;
import com.backbase.stream.product.service.ArrangementService;
import com.backbase.stream.product.task.ProductGroupTask;
import com.backbase.stream.product.utils.StreamUtils;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.UserService;
import com.backbase.stream.worker.exception.StreamTaskException;
import com.backbase.stream.worker.model.StreamTask;
import io.micrometer.tracing.annotation.ContinueSpan;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Manage Products (In DBS Called Arrangements).
 */
@Slf4j
public class ProductIngestionSaga {

    public static final String IDENTITY_USER = "IDENTITY_USER";
    public static final String USER = "USER";
    public static final String REJECTED = "rejected";
    public static final String PRODUCT_GROUP = "product-group";
    public static final String FUNCTION_GROUP = "function-group";
    public static final String PROCESS = "process";
    public static final String VALIDATE = "validate";
    public static final String ARRANGEMENT = "arrangement";
    public static final String UPSERT_ARRANGEMENT = "upsert-arrangement";
    public static final String EXISTS = "exists";
    public static final String UPDATE_ARRANGEMENT = "update-arrangement";
    public static final String FAILED = "failed";
    public static final String UPDATED = "updated";
    public static final String REMOVED = "removed";
    private static final String CREATED = "created";

    protected final ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);

    protected final BusinessFunctionGroupMapper businessFunctionGroupMapper = Mappers.getMapper(BusinessFunctionGroupMapper.class);

    protected final ArrangementService arrangementService;
    protected final AccessGroupService accessGroupService;
    protected final UserService userService;
    protected final ProductIngestionSagaConfigurationProperties configurationProperties;
    protected final LoansSaga loansSaga;

    public ProductIngestionSaga(ArrangementService arrangementService, AccessGroupService accessGroupService, UserService userService, ProductIngestionSagaConfigurationProperties configurationProperties, LoansSaga loansSaga) {
        this.arrangementService = arrangementService;
        this.accessGroupService = accessGroupService;
        this.userService = userService;
        this.configurationProperties = configurationProperties;
        this.loansSaga = loansSaga;
    }

    @ContinueSpan(log = "processProducts")
    public Mono<ProductGroupTask> process(ProductGroupTask streamTask) {
        return validateProductGroup(streamTask)
            .doOnNext(productGroupTask -> {
                streamTask.info(PRODUCT_GROUP, PROCESS, null, streamTask.getProductGroup().getName(), null, "Process Product Group Task: %s", productGroupTask.getId());
            })
            .flatMap(this::upsertArrangements)
            .flatMap(accessGroupService::setupProductGroups)
            .flatMap(this::setupBusinessFunctionsAndPermissions);
    }

    private Mono<? extends ProductGroupTask> setupBusinessFunctionsAndPermissions(ProductGroupTask streamTask) {
        ProductGroup productGroup = streamTask.getData();
        return Flux.fromIterable(productGroup.getUsers())
            .flatMap(jobProfileUser -> setupBusinessFunctions(streamTask, streamTask.getData().getServiceAgreement(), jobProfileUser))
            .flatMap(jobProfileUser -> setupPermissions(streamTask, jobProfileUser))
            .collectList()
            .map(productGroup::users)
            .cast(ProductGroup.class)
            .map(streamTask::data);
    }

    private Mono<ProductGroupTask> validateProductGroup(ProductGroupTask streamTask) {
        ProductGroup productGroup = streamTask.getData();
        String name = productGroup.getName();
        List<CustomDataGroupItem> customDataGroupItems = productGroup.getCustomDataGroupItems();
        if (productGroup.getUsers() == null) {
            streamTask.warn(PRODUCT_GROUP, VALIDATE, REJECTED, name, null, "Product Group must have users assigned to it!");
            return Mono.error(new StreamTaskException(streamTask, "Product Group must have users assigned to it!"));
        }
        if (StreamUtils.getInternalProductIds(productGroup).isEmpty() && customDataGroupItems.isEmpty()) {
            streamTask.warn(PRODUCT_GROUP, VALIDATE, REJECTED, name, null, "Product group must have products or Custom Data Group Items assigned to it!");
            return Mono.error(new StreamTaskException(streamTask, "Product group must have products or Custom Data Group Items assigned to it!"));
        }
        if (customDataGroupItems != null && !customDataGroupItems.isEmpty() && productGroup.getProductGroupType() == null) {
            streamTask.warn(PRODUCT_GROUP, VALIDATE, REJECTED, name, null, "Product Group with Custom Data Group Items must have a Product Group Defined!");
            return Mono.error(new StreamTaskException(streamTask, "Product Group with Custom Data Group Items must have a Product Group Defined!"));
        }
        if (productGroup.getProductGroupType() == null && StreamUtils.getAllProducts(productGroup).findAny().isPresent()) {
            streamTask.warn(PRODUCT_GROUP, VALIDATE, REJECTED, name, null, "Product Group: {} does not have type setup. As Product Group contains products setting type to ARRANGEMENTS");
            productGroup.setProductGroupType(ProductGroup.ProductGroupTypeEnum.ARRANGEMENTS);
        }

        return Mono.just(streamTask);
    }

    private Mono<JobProfileUser> setupBusinessFunctions(StreamTask streamTask, ServiceAgreement serviceAgreement,
                                                        JobProfileUser jobProfileUser) {
        streamTask
            .info(FUNCTION_GROUP, "setup-business-functions", "", "", null, "Setting up Business Functions User: %s",
                jobProfileUser.getUser().getExternalId());
        return getBusinessFunctionGroups(jobProfileUser, serviceAgreement)
            .flatMap(businessFunctionGroups -> accessGroupService.setupFunctionGroups(streamTask, serviceAgreement, businessFunctionGroups))
            .map(jobProfileUser::businessFunctionGroups);
    }

    protected Mono<List<BusinessFunctionGroup>> getBusinessFunctionGroups(JobProfileUser jobProfileUser, ServiceAgreement serviceAgreement) {

        List<BusinessFunctionGroup> businessFunctionGroups = jobProfileUser.getBusinessFunctionGroups();
        if (!isEmpty(jobProfileUser.getReferenceJobRoleNames())) {
            return accessGroupService.getFunctionGroupsForServiceAgreement(serviceAgreement.getInternalId())
                .map(functionGroups -> {
                    Map<String, FunctionGroupItem> idByFunctionGroupName = functionGroups.stream()
                        .collect(toMap(FunctionGroupItem::getName, Function.identity()));
                    return jobProfileUser.getReferenceJobRoleNames().stream()
                        .map(idByFunctionGroupName::get)
                        .filter(Objects::nonNull)
                        .map(businessFunctionGroupMapper::map)
                        .collect(Collectors.toList());
                })
                .map(bf -> {
                    if (!isEmpty(businessFunctionGroups))
                        bf.addAll(businessFunctionGroups);
                    return bf;
                });
        }
        return Mono.justOrEmpty(businessFunctionGroups);
    }

    /***********
     * FIX ME
     */
//    private Mono<JobProfileUser> setupPermissions(ProductGroupTask streamTask, JobProfileUser jobProfileUser) {
//        return processUser(streamTask, jobProfileUser)
//            .map(jobProfileUser::user)
//            .flatMap(actual -> {
//                List<BusinessFunctionGroup> businessFunctionGroups = jobProfileUser.getBusinessFunctionGroups();
//
//                Map<User, Map<BusinessFunctionGroup, List<BaseProductGroup>>> request = new HashMap<>();
//                request.put(jobProfileUser.getUser(),businessFunctionGroups.stream()
//                    .collect(Collectors.toMap(
//                        bfg -> bfg,
//                        bfg -> Collections.emptyList()
//                    )) );
//
//                return accessGroupService.assignPermissionsBatch(
//                    new BatchProductGroupTask(streamTask.getId(), new BatchProductGroup()
//                        .serviceAgreement(streamTask.getProductGroup().getServiceAgreement()), BatchProductGroupTask.IngestionMode.UPDATE), request)
//                    .thenReturn(jobProfileUser);
//            });
//    }

    private Mono<JobProfileUser> setupPermissions(ProductGroupTask streamTask, JobProfileUser jobProfileUser) {
        return processUser(streamTask, jobProfileUser)
            .map(jobProfileUser::user)
            .flatMap(actualUser -> accessGroupService.assignPermissions(streamTask, jobProfileUser));
    }

    protected Mono<User> processUser(StreamTask streamTask, JobProfileUser jobProfileUser) {
        if (configurationProperties.isIdentityEnabled()
            && !IdentityUserLinkStrategy.IDENTITY_AGNOSTIC.equals(jobProfileUser.getUser().getIdentityLinkStrategy())) {
            return upsertIdentityUser(streamTask, jobProfileUser);
        } else {
            log.debug("Fallback to Identity Agnostic identityLinkStrategy. Either identity integration is disabled or User identityLinkStrategy is not set to identity.");
            return upsertUser(streamTask, jobProfileUser);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private Mono<User> upsertUser(StreamTask streamTask, JobProfileUser jobProfileUser) {
        User user = jobProfileUser.getUser();
        LegalEntityReference legalEntityReference = jobProfileUser.getLegalEntityReference();
        Mono<User> getExistingUser = Objects.nonNull(user.getInternalId())? userService.getUserById(user.getInternalId()): userService.getUserByExternalId(user.getExternalId())
            .doOnNext(existingUser -> streamTask.info(USER, EXISTS, user.getExternalId(), user.getInternalId(), "User %s already exists", existingUser.getExternalId()));
        Mono<User> createNewUser = userService.createUser(user, legalEntityReference.getExternalId(), streamTask)
            .doOnNext(existingUser -> streamTask.info(USER, CREATED, user.getExternalId(), user.getInternalId(), "User %s created", existingUser.getExternalId()));
        return getExistingUser.switchIfEmpty(createNewUser);
    }

    @SuppressWarnings("DuplicatedCode")
    private Mono<User> upsertIdentityUser(StreamTask streamTask, JobProfileUser jobProfileUser) {
        User user = jobProfileUser.getUser();
        LegalEntityReference legalEntityReference = jobProfileUser.getLegalEntityReference();
        Mono<User> getExistingIdentityUser = Objects.nonNull(user.getInternalId())? userService.getUserById(user.getInternalId()): userService.getUserByExternalId(user.getExternalId())
            .doOnNext(existingUser -> streamTask.info(IDENTITY_USER, EXISTS, user.getExternalId(), user.getInternalId(), "User %s already exists", existingUser.getExternalId()));
        Mono<User> createNewIdentityUser = userService.createOrImportIdentityUser(user, legalEntityReference.getInternalId(), streamTask)
            .doOnNext(existingUser -> streamTask.info(IDENTITY_USER, CREATED, user.getExternalId(), user.getInternalId(), "User %s created", existingUser.getExternalId()));
        return getExistingIdentityUser.switchIfEmpty(createNewIdentityUser);
    }

    @SuppressWarnings("DuplicatedCode")
    public Mono<ProductGroupTask> upsertArrangements(ProductGroupTask streamTask) {
        ProductGroup productGroup = streamTask.getData();

        streamTask.info(PRODUCT_GROUP, PROCESS, null, streamTask.getProductGroup().getName(), null, "Process Product Group Arrangements: %s", StreamUtils.getExternalProductIds(productGroup));

        // Ensure all product types are of valid groups
        Flux<CurrentAccount> currentAccountFlux = getCurrentAccountFlux(productGroup);
        Flux<SavingsAccount> savingsAccountFlux = getSavingAccountsFlux(productGroup);
        Flux<DebitCard> debitCardFlux = getDebitCardFlux(productGroup);
        Flux<CreditCard> creditCardFlux = getCreditCardFlux(productGroup);
        Flux<Loan> loanFlux = getLoanFlux(productGroup);
        Flux<TermDeposit> termDepositFlux = getTermDepositFlux(productGroup);
        Flux<InvestmentAccount> investmentAccountFlux = getInvestmentAccountFlux(productGroup);
        Flux<Product> productFlux = getProductFlux(productGroup);


        Mono<List<ArrangementItem>> currentAccountsRequests = upsertArrangements(streamTask, currentAccountFlux.map(productMapper::toPresentation));
        Mono<List<ArrangementItem>> savingAccountsRequests = upsertArrangements(streamTask, savingsAccountFlux.map(productMapper::toPresentation));
        Mono<List<ArrangementItem>> debitCardsRequests = upsertArrangements(streamTask, debitCardFlux.map(productMapper::toPresentation));
        Mono<List<ArrangementItem>> creditCardsRequests = upsertArrangements(streamTask, creditCardFlux.map(productMapper::toPresentation));
        Mono<List<ArrangementItem>> loansRequests = upsertArrangements(streamTask, loanFlux.map(productMapper::toPresentation));
        Mono<List<ArrangementItem>> termDepositsRequests = upsertArrangements(streamTask, termDepositFlux.map(productMapper::toPresentation));
        Mono<List<ArrangementItem>> investmentAccountsRequests = upsertArrangements(streamTask, investmentAccountFlux.map(productMapper::toPresentation));
        Mono<List<ArrangementItem>> customProductsRequests = upsertArrangements(streamTask, productFlux.map(productMapper::toPresentation));

        return Mono.just(productGroup)
            .zipWith(currentAccountsRequests, (actual, arrangements) -> {
                List<CurrentAccount> collect = arrangements.stream().map(productMapper::mapCurrentAccount).toList();
                return actual.currentAccounts(collect);
            })
            .zipWith(savingAccountsRequests, (actual, arrangements) -> actual.savingAccounts(arrangements.stream().map(productMapper::mapSavingAccount).toList()))
            .zipWith(debitCardsRequests, (actual, arrangements) -> actual.debitCards(arrangements.stream().map(productMapper::mapDebitCard).toList()))
            .zipWith(creditCardsRequests, (actual, arrangements) -> actual.creditCards(arrangements.stream().map(productMapper::mapCreditCard).toList()))
            .zipWith(loansRequests, (actual, arrangements) -> actual.loans(arrangements.stream().map(productMapper::mapLoan).toList()))
            .zipWith(termDepositsRequests, (actual, arrangements) -> actual.termDeposits(arrangements.stream().map(productMapper::mapTermDeposit).toList()))
            .zipWith(investmentAccountsRequests, (actual, arrangements) -> actual.investmentAccounts(arrangements.stream().map(productMapper::mapInvestmentAccount).toList()))
            .zipWith(customProductsRequests, (actual, arrangements) -> actual.customProducts(arrangements.stream().map(productMapper::mapCustomProduct).toList()))
            .cast(ProductGroup.class)
            .map(streamTask::data);
    }

    private Mono<List<ArrangementItem>> upsertArrangements(ProductGroupTask streamTask, Flux<PostArrangement> productFlux) {
        ProductGroup productGroup = streamTask.getData();
        return productFlux
            .map(p -> ensureLegalEntityId(productGroup.getUsers(), p))
            .sort(comparing(PostArrangement::getParentId, nullsFirst(naturalOrder()))) // Avoiding child to be created before parent
            .flatMapSequential(arrangementItemPost -> upsertArrangement(streamTask, arrangementItemPost))
            .collectList();
    }

    /**
     * Create a postArrangement in DBS. If postArrangement already exists, return existing postArrangement
     *
     * @param postArrangement Product to create
     * @return Created or Existing Product
     */
    public Mono<ArrangementItem> upsertArrangement(ProductGroupTask streamTask, PostArrangement postArrangement) {
        streamTask.info(ARRANGEMENT, UPSERT_ARRANGEMENT, "", postArrangement.getId(), null, "Inserting or updating arrangement: %s", postArrangement.getId());
        log.info("Upsert Arrangement: {} in Product Group: {}", postArrangement.getId(), streamTask.getData().getName());
        Mono<ArrangementItem> updateArrangement = arrangementService.getArrangementInternalId(postArrangement.getId())
            .flatMap(internalId -> {
                String internalIds = String.join(",", internalId);
                log.info("Arrangement already exists: {}", internalId);
                streamTask.info(ARRANGEMENT, UPSERT_ARRANGEMENT, EXISTS, postArrangement.getId(), internalIds, "Arrangement %s already exists", postArrangement.getId());
                ArrangementPutItem arrangemenItemBase = productMapper.toArrangementItemPut(postArrangement);
                return arrangementService.updateArrangement(internalId, arrangemenItemBase)
                    .onErrorResume(ArrangementUpdateException.class, e -> {
                        streamTask.error(ARRANGEMENT, UPDATE_ARRANGEMENT, FAILED, postArrangement.getId(), internalIds, e, e.getHttpResponse(), "Failed to update arrangement: %s", postArrangement.getId());
                        return Mono.error(new StreamTaskException(streamTask, e.getCause(),
                            e.getMessage() + " " + e.getCause().getMessage()));
                    })
                    .map(actual -> {
                        log.info("Updated arrangement: {}", actual.getExternalArrangementId());
                        streamTask.info(ARRANGEMENT, UPSERT_ARRANGEMENT, UPDATED, postArrangement.getId(), internalId, "Updated Arrangement");
                        ArrangementItem arrangementItem = productMapper.toArrangementItem(actual);
                        arrangementItem.setId(internalId);
                        return arrangementItem;
                    });
            });

            Mono<ArrangementItem> createNewArrangement = createArrangement(postArrangement)
                .map(arrangementItem -> {
                    streamTask.info(ARRANGEMENT, "insert-arrangement", CREATED, postArrangement.getId(), arrangementItem.getId(), "Created Arrangement");
                    log.info("Created arrangement: {} with internalId: {} ", arrangementItem.getExternalArrangementId(), arrangementItem.getId());
                    return arrangementItem;
                })
                .onErrorResume(ArrangementCreationException.class, e -> {
                    streamTask.error(ARRANGEMENT, "insert-arrangement", FAILED, postArrangement.getId(), null, e, e.getHttpResponse(), "Failed to update arrangement: %s", postArrangement.getId());
                    return Mono.error(new StreamTaskException(streamTask, e, "Failed to create arrangement"));
                });

        return updateArrangement.switchIfEmpty(createNewArrangement);
    }

    private Mono<ArrangementItem> createArrangement(PostArrangement postArrangement) {
        return arrangementService.createArrangement(postArrangement)
            .doOnError(WebClientResponseException.class, throwable ->
                log.error("Failed to create product: {}\n{}", postArrangement.getId(), throwable.getResponseBodyAsString()))
            .map(arrangementAddedResponse -> {
                ArrangementItem arrangementItem = productMapper.toArrangementItem(postArrangement);
                arrangementItem.setId(arrangementAddedResponse.getId());
                return arrangementItem;
            });
    }

    private Flux<Product> getProductFlux(ProductGroup productGroup) {
        return productGroup.getCustomProducts() == null
            ? Flux.empty()
            : Flux.fromIterable(productGroup.getCustomProducts());
    }

    private Flux<InvestmentAccount> getInvestmentAccountFlux(ProductGroup productGroup) {
        return productGroup.getInvestmentAccounts() == null
            ? Flux.empty()
            : Flux.fromIterable(productGroup.getInvestmentAccounts());
    }

    private Flux<TermDeposit> getTermDepositFlux(ProductGroup productGroup) {
        return productGroup.getTermDeposits() == null
            ? Flux.empty()
            : Flux.fromIterable(productGroup.getTermDeposits());
    }

    private Flux<Loan> getLoanFlux(ProductGroup productGroup) {
        return productGroup.getLoans() == null
            ? Flux.empty()
            : Flux.fromIterable(productGroup.getLoans());
    }

    private Flux<CreditCard> getCreditCardFlux(ProductGroup productGroup) {
        return productGroup.getCreditCards() == null
            ? Flux.empty()
            : Flux.fromIterable(productGroup.getCreditCards());
    }

    private Flux<DebitCard> getDebitCardFlux(ProductGroup productGroup) {
        return productGroup.getDebitCards() == null
            ? Flux.empty()
            : Flux.fromIterable(productGroup.getDebitCards());
    }

    private Flux<SavingsAccount> getSavingAccountsFlux(ProductGroup productGroup) {
        return productGroup.getSavingAccounts() == null
            ? Flux.empty()
            : Flux.fromIterable(productGroup.getSavingAccounts());
    }

    private Flux<CurrentAccount> getCurrentAccountFlux(ProductGroup productGroup) {
        return productGroup.getCurrentAccounts() == null
            ? Flux.empty()
            : Flux.fromIterable(productGroup.getCurrentAccounts());
    }

    protected PostArrangement ensureLegalEntityId(List<JobProfileUser> users, PostArrangement product) {
        Set<String> legalEntityExternalIds = users.stream()
            .map(jobProfileUser -> jobProfileUser.getLegalEntityReference().getExternalId())
            .collect(Collectors.toSet());
        // Make sure that we take into consideration Legal Entity Ids provided in Arrangement Item itself.
        if (!isEmpty(product.getLegalEntityIds())) {
            legalEntityExternalIds.addAll(product.getLegalEntityIds());
        }
        product.setLegalEntityIds(new HashSet<>(legalEntityExternalIds));
        return product;
    }

}
