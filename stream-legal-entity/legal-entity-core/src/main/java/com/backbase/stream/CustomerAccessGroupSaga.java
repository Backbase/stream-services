package com.backbase.stream;

import com.backbase.dbs.accesscontrol.api.service.v3.FunctionGroupsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.model.FunctionGroupItem;
import com.backbase.stream.worker.exception.StreamTaskException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.CustomerAccessGroup;
import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.CustomerAccessGroupItem;
import com.backbase.stream.configuration.CustomerAccessGroupConfigurationProperties;
import com.backbase.stream.legalentity.model.LegalEntityV2;
import com.backbase.stream.legalentity.model.ServiceAgreementV2;
import com.backbase.stream.service.CustomerAccessGroupService;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.model.StreamTask;

import io.micrometer.tracing.annotation.SpanTag;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
        log.info("Assigning {} Customer Access Groups to Legal Entity: {}", cagNames.size(),
            legalEntity.getExternalId());

        if (!CollectionUtils.isEmpty(cagNames)) {
            var cagIds = getCustomerAccessGroupIdsMatchingNames(streamTask, cagNames, true);

            cagService.assignCustomerAccessGroupsToLegalEntity(
                streamTask, legalEntity, cagIds);
        }
        return Mono.just(streamTask);
    }

    public Mono<ServiceAgreementTaskV2> assignCustomerAccessGroupsToJobRoles(
        @SpanTag(value = "streamTask") ServiceAgreementTaskV2 streamTask) {
        ServiceAgreementV2 serviceAgreement = streamTask.getServiceAgreement();
        log.info("Processing Customer Access Groups for Service Agreement: {}", serviceAgreement.getExternalId());
        List<FunctionGroupItem> functionGroupInSa = getFunctionGroupsInServiceAgreement(
            serviceAgreement.getInternalId());
        serviceAgreement.getJobProfileUsers().stream()
            .filter(
                jobProfileUser -> !CollectionUtils.isEmpty(jobProfileUser.getJobRoleNameToCustomerAccessGroupNames()))
            .forEach(jobProfileUser -> {
                Map<String, Set<Long>> jobRoleToCagsMap = new HashMap<>();
                jobProfileUser.getJobRoleNameToCustomerAccessGroupNames().stream()
                    .filter(jobRoleToCag -> !CollectionUtils.isEmpty(jobRoleToCag.getCustomerAccessGroupNames()))
                    .forEach(jobRoleToCag -> {
                        String fgId = functionGroupInSa.stream()
                            .filter(fg -> fg.getName().equalsIgnoreCase(jobRoleToCag.getJobRoleName()))
                            .findFirst()
                            .orElseThrow(
                                () -> new NoSuchElementException(
                                    String.format("Function group with name '%s' not exist",
                                        jobRoleToCag.getJobRoleName())))
                            .getId();
                        Set<Long> cagIds = getCustomerAccessGroupIdsMatchingNames(streamTask,
                            jobRoleToCag.getCustomerAccessGroupNames(), true);
                        jobRoleToCagsMap.put(fgId, cagIds);

                    });
                cagService.assignCustomerAccessGroupsToJobRoles(streamTask,
                    jobProfileUser.getUser().getInternalId(), serviceAgreement, jobRoleToCagsMap);
            });
        return Mono.just(streamTask);
    }

    private List<FunctionGroupItem> getFunctionGroupsInServiceAgreement(String serviceAgreementId) {
        return functionGroupsApi.getFunctionGroups(serviceAgreementId).collectList().block();
    }

    private Mono<CustomerAccessGroupTask> setUpCustomerAccessGroup(CustomerAccessGroupTask streamTask) {
        CustomerAccessGroupItem cag = streamTask.getCustomerAccessGroup();

        CustomerAccessGroup request = new CustomerAccessGroup()
            .name(cag.getName()).description(cag.getDescription()).mandatory(cag.getMandatory());
        Set<Long> matchedCag = getCustomerAccessGroupIdsMatchingNames(streamTask, List.of(cag.getName()), false);
        if (matchedCag.isEmpty()) {
            log.info("Creating new Customer Access Group");
            return cagService.createCustomerAccessGroup(streamTask, request)
                .map(createdGroup -> {
                    streamTask.setCustomerAccessGroup(createdGroup);
                    log.info("Created Customer Access Group: {}", createdGroup.getId());
                    return streamTask;
                });
        }

        log.info("Updating Customer Access Group: {}", matchedCag.iterator().next());
        cagService.updateCustomerAccessGroup(streamTask, matchedCag.iterator().next(), request);
        cag.setId(matchedCag.iterator().next());
        return Mono.just(streamTask);
    }

    private Set<Long> getCustomerAccessGroupIdsMatchingNames(StreamTask streamTask, List<String> cagNames,
        boolean validateNames) {
        return cagService.getCustomerAccessGroups(streamTask)
            .flatMapMany(Flux::fromIterable)
            .filter(cag -> cagNames.contains(cag.getName()))
            .collect(Collectors.toMap(CustomerAccessGroupItem::getId, CustomerAccessGroupItem::getName))
            .flatMap(cagMap -> {
                if (validateNames) {
                    Set<String> diffCagNames = cagNames.stream().filter(name -> !cagMap.containsValue(name))
                        .collect(Collectors.toSet());

                    if (!diffCagNames.isEmpty()) {
                        return Mono.error(new StreamTaskException(streamTask,
                            String.format("Customer access group names '%s' not exist", diffCagNames)));
                    }
                }
                return Mono.just(cagMap.keySet());
            })
            .block();
    }

}