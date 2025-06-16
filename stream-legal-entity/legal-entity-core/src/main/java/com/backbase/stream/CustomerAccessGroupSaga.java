package com.backbase.stream;

import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.CustomerAccessGroup;
import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.CustomerAccessGroupItem;
import com.backbase.dbs.accesscontrol.api.service.v3.FunctionGroupsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.model.FunctionGroupItem;
import com.backbase.stream.configuration.CustomerAccessGroupConfigurationProperties;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.JobRoleNameToCustomerAccessGroupNames;
import com.backbase.stream.legalentity.model.LegalEntityV2;
import com.backbase.stream.legalentity.model.ServiceAgreementV2;
import com.backbase.stream.service.CustomerAccessGroupService;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.exception.StreamTaskException;
import com.backbase.stream.worker.model.StreamTask;
import io.micrometer.tracing.annotation.SpanTag;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Component
@Slf4j
public class CustomerAccessGroupSaga implements StreamTaskExecutor<CustomerAccessGroupTask> {

    private final CustomerAccessGroupService cagService;
    private final CustomerAccessGroupConfigurationProperties cagConfig;
    private final FunctionGroupsApi functionGroupsApi;

    public CustomerAccessGroupSaga(CustomerAccessGroupService cagService,
        CustomerAccessGroupConfigurationProperties cagConfig, FunctionGroupsApi functionGroupsApi) {
        this.cagService = cagService;
        this.cagConfig = cagConfig;
        this.functionGroupsApi = functionGroupsApi;
    }

    public boolean isEnabled() {
        return cagConfig.enabled();
    }

    @Override
    public Mono<CustomerAccessGroupTask> executeTask(
        @SpanTag(value = "streamTask") CustomerAccessGroupTask streamTask) {
        if (isEnabled()) {
            return setUpCustomerAccessGroup(streamTask);
        } else {
            log.info("Skipping creation of Customer Access Groups - feature is disabled");
            return Mono.just(streamTask);
        }
    }

    @Override
    public Mono<CustomerAccessGroupTask> rollBack(CustomerAccessGroupTask streamTask) {
        // GET CREATED AND EVENTS AND CALL DELETE ENDPOINTS IN REVERSE
        return Mono.just(streamTask);
    }

    public Mono<LegalEntityTaskV2> assignCustomerAccessGroupsToLegalEntity(
        @SpanTag(value = "streamTask") LegalEntityTaskV2 streamTask) {
        LegalEntityV2 legalEntity = streamTask.getData();
        var cagNames = legalEntity.getCustomerAccessGroupNames();
        if (CollectionUtils.isEmpty(cagNames)) {
            return Mono.just(streamTask);
        }
        log.info("Assigning {} Customer Access Groups to Legal Entity: {}", cagNames.size(),
            legalEntity.getExternalId());
        return getCustomerAccessGroupIdsMatchingNames(streamTask, cagNames)
            .flatMap(cagMap -> cagService.assignCustomerAccessGroupsToLegalEntity(streamTask, legalEntity,
                new HashSet<>(cagMap.values())))
            .doOnNext(legalEntityTask -> log.info("Assigned Customer Access Groups {} to Legal Entity {}",
                legalEntityTask.getCustomerAccessGroupNames(), legalEntityTask.getName()))
            .then(Mono.just(streamTask));
    }

    public Mono<ServiceAgreementTaskV2> assignCustomerAccessGroupsToJobRoles(
        @SpanTag(value = "streamTask") ServiceAgreementTaskV2 streamTask) {
        ServiceAgreementV2 serviceAgreement = streamTask.getServiceAgreement();
        List<JobProfileUser> usersToBeAssignedWithCags = serviceAgreement.getJobProfileUsers().stream()
            .filter(
                jobProfileUser -> !CollectionUtils.isEmpty(jobProfileUser.getJobRoleNameToCustomerAccessGroupNames()))
            .toList();
        if (usersToBeAssignedWithCags.isEmpty()) {
            return Mono.just(streamTask);
        }
        log.info("Processing Customer Access Groups for {} users in Service Agreement: {}",
            usersToBeAssignedWithCags.size(), serviceAgreement.getExternalId());

        List<String> cagNames = usersToBeAssignedWithCags.stream()
            .map(JobProfileUser::getJobRoleNameToCustomerAccessGroupNames)
            .flatMap(Collection::stream)
            .map(JobRoleNameToCustomerAccessGroupNames::getCustomerAccessGroupNames)
            .flatMap(Collection::stream)
            .toList();

        Mono<Map<String, String>> fgNamesToIdsMono = getFunctionGroupsInSaNameToIdMap(streamTask);
        Mono<Map<String, Long>> cagNamesToIdsMono = getCustomerAccessGroupIdsMatchingNames(streamTask, cagNames);
        return Mono.zip(fgNamesToIdsMono, cagNamesToIdsMono)
            .flatMapMany(fgAndCagDataTuple -> Flux.concat(
                usersToBeAssignedWithCags.stream().map(user -> {
                    Map<String, Set<Long>> jobRoleIdToCagIds = covertJobRoleAndCagNamesToIds(fgAndCagDataTuple, user);
                    log.info("Assigning CAG permissions to User {} in Service Agreement {}",
                        user.getUser().getExternalId(), serviceAgreement.getExternalId());
                    return cagService.assignCustomerAccessGroupsToJobRoles(streamTask, user, serviceAgreement,
                        jobRoleIdToCagIds);
                }).collect(Collectors.toSet())
            ))
            .doOnNext(i -> log.info("Assigned CAG permissions to User {} in SA {}", i.getUser().getExternalId(),
                serviceAgreement.getExternalId()))
            .then(Mono.just(streamTask));
    }

    private static Map<String, Set<Long>> covertJobRoleAndCagNamesToIds(
        Tuple2<Map<String, String>, Map<String, Long>> tuple, JobProfileUser user) {
        return user.getJobRoleNameToCustomerAccessGroupNames().stream()
            .collect(Collectors.toMap(
                jobRoleToCag -> Optional.ofNullable(tuple.getT1().get(jobRoleToCag.getJobRoleName()))
                    .orElseThrow(() -> new NoSuchElementException(
                        String.format("Function group with name '%s' does not exist", jobRoleToCag.getJobRoleName()))),
                jobRoleToCag -> jobRoleToCag.getCustomerAccessGroupNames().stream()
                    .map(cagName -> tuple.getT2().get(cagName)).collect(Collectors.toSet())
            ));
    }

    private Mono<CustomerAccessGroupTask> setUpCustomerAccessGroup(CustomerAccessGroupTask streamTask) {
        CustomerAccessGroupItem cag = streamTask.getCustomerAccessGroup();

        CustomerAccessGroup request = new CustomerAccessGroup()
            .name(cag.getName()).description(cag.getDescription()).mandatory(cag.getMandatory());
        return getAllCustomerAccessGroupsNameToIdMap()
            .flatMap(existingCagNameToIdMap -> {
                if (!existingCagNameToIdMap.containsKey(cag.getName())) {
                    log.info("Creating new Customer Access Group {}", cag.getName());
                    return cagService.createCustomerAccessGroup(streamTask, request);
                }

                Long existingCagId = existingCagNameToIdMap.get(cag.getName());
                log.info("Updating Customer Access Group: {}", existingCagId);
                return cagService.updateCustomerAccessGroup(streamTask, existingCagId, request);
            })
            .doOnNext(item -> log.info("Customer Access Group {} ingested with ID {}", item.getName(), item.getId()))
            .then(Mono.just(streamTask));
    }

    public Mono<Map<String, Long>> getCustomerAccessGroupIdsMatchingNames(StreamTask streamTask,
        List<String> cagNames) {
        return getAllCustomerAccessGroupsNameToIdMap()
            .handle((cagMap, sink) -> {
                if (!cagMap.keySet().containsAll(cagNames)) {
                    Set<String> invalidCagNames = cagNames.stream().filter(name -> !cagMap.containsKey(name))
                        .collect(Collectors.toSet());
                    sink.error(new StreamTaskException(streamTask,
                        String.format("Customer access groups '%s' do not exist", invalidCagNames)));
                    return;
                }
                sink.next(cagMap.entrySet().stream().filter(entry -> cagNames.contains(entry.getKey())).collect(
                    Collectors.toMap(Entry::getKey, Entry::getValue)));
            });
    }

    private Mono<Map<String, Long>> getAllCustomerAccessGroupsNameToIdMap() {
        return cagService.getCustomerAccessGroups()
            .map(cagList -> cagList.stream()
                .collect(Collectors.toMap(CustomerAccessGroupItem::getName, CustomerAccessGroupItem::getId)));
    }

    private Mono<Map<String, String>> getFunctionGroupsInSaNameToIdMap(ServiceAgreementTaskV2 streamTask) {
        return functionGroupsApi.getFunctionGroups(streamTask.getServiceAgreement().getInternalId())
            .collectMap(FunctionGroupItem::getName, FunctionGroupItem::getId);
    }
}