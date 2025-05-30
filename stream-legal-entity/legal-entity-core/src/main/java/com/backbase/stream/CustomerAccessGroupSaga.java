package com.backbase.stream;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.CustomerAccessGroup;
import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.CustomerAccessGroupItem;
import com.backbase.stream.configuration.CustomerAccessGroupConfigurationProperties;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.LegalEntityV2;
import com.backbase.stream.legalentity.model.ServiceAgreementV2;
import com.backbase.stream.legalentity.model.User;
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

    public CustomerAccessGroupSaga(CustomerAccessGroupService cagService,
        CustomerAccessGroupConfigurationProperties cagConfig) {
        this.cagService = cagService;
        this.cagConfig = cagConfig;
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
            var cagIds = getCustomerAccessGroupIdsMatchingNames(streamTask, cagNames);

            cagService.assignCustomerAccessGroupsToLegalEntity(
                streamTask, legalEntity, cagIds);
        }
        return Mono.just(streamTask);
    }

    public Mono<ServiceAgreementTaskV2> assignCustomerAccessGroupsToJobRoles(
        @SpanTag(value = "streamTask") ServiceAgreementTaskV2 streamTask,
        Map<User, Map<BusinessFunctionGroup, List<BaseProductGroup>>> map) {
        ServiceAgreementV2 serviceAgreement = streamTask.getServiceAgreement();
        log.info("Processing Customer Access Groups for Service Agreement: {}", serviceAgreement.getExternalId());
        serviceAgreement.getJobProfileUsers().stream()
            .filter(jobProfileUser -> !CollectionUtils.isEmpty(jobProfileUser.getCustomerAccessGroupNames()))
            .forEach(jobProfileUser -> {
                var entry = map.get(jobProfileUser.getUser());
                var cagIds = getCustomerAccessGroupIdsMatchingNames(streamTask,
                    jobProfileUser.getCustomerAccessGroupNames());

                Map<String, Set<Long>> fgIdToCagIds = entry.keySet().stream()
                    .collect(Collectors.toMap(BusinessFunctionGroup::getId, bfg -> cagIds));

                cagService.assignCustomerAccessGroupsToJobRoles(streamTask, jobProfileUser.getUser().getInternalId(),
                    serviceAgreement, fgIdToCagIds);
            });
        return Mono.just(streamTask);
    }

    private Mono<CustomerAccessGroupTask> setUpCustomerAccessGroup(CustomerAccessGroupTask streamTask) {
        CustomerAccessGroupItem cag = streamTask.getCustomerAccessGroup();

        CustomerAccessGroup request = new CustomerAccessGroup()
            .name(cag.getName()).description(cag.getDescription()).mandatory(cag.getMandatory());
        Set<Long> matchedCag = getCustomerAccessGroupIdsMatchingNames(streamTask, List.of(cag.getName()));
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

    private Set<Long> getCustomerAccessGroupIdsMatchingNames(StreamTask streamTask, List<String> cagNames) {
        return cagService.getCustomerAccessGroups(streamTask)
            .flatMapMany(Flux::fromIterable)
            .filter(cag -> cagNames.contains(cag.getName()))
            .map(CustomerAccessGroupItem::getId)
            .collect(Collectors.toSet())
            .block();
    }

}