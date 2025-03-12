package com.backbase.stream.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.CustomerAccessGroupApi;
import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.CustomerAccessGroup;
import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.CustomerAccessGroupItem;
import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.CustomerAccessGroupUserPermissionItem;
import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.GetCustomerAccessGroups;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityV2;
import com.backbase.stream.legalentity.model.ServiceAgreementV2;
import com.backbase.stream.worker.exception.StreamTaskException;
import com.backbase.stream.worker.model.StreamTask;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class CustomerAccessGroupService {

    private static final String CUSTOMER_ACCESS_GROUP = "customer-access-group";

    @NotNull
    private final CustomerAccessGroupApi customerAccessGroupApi;

    public Mono<CustomerAccessGroupItem> createCustomerAccessGroup(StreamTask streamTask, CustomerAccessGroup customerAccessGroup) {
        streamTask.info(CUSTOMER_ACCESS_GROUP, "create", "", customerAccessGroup.getName(), null, "Creating Customer Access Group: %s", customerAccessGroup.getName());

        CustomerAccessGroupItem cagItem = new CustomerAccessGroupItem();
        cagItem.setName(customerAccessGroup.getName());
        cagItem.setDescription(customerAccessGroup.getDescription());
        cagItem.setMandatory(customerAccessGroup.getMandatory());
        return customerAccessGroupApi.createCustomerAccessGroup(customerAccessGroup)
            .onErrorResume(WebClientResponseException.class, throwable -> {
                streamTask.error(CUSTOMER_ACCESS_GROUP, "create", "failed", customerAccessGroup.getName(),
                    "", throwable, throwable.getResponseBodyAsString(), "Failed to create Customer Access Group");
                return Mono.error(new StreamTaskException(streamTask, throwable, "Failed to create Customer Access Group"));
            })
            .map(createdCustomerAccessGroupId -> {
                log.info("Created Customer Access Group: {} with ID: {}", customerAccessGroup.getName(), createdCustomerAccessGroupId.getId());
                cagItem.setId(createdCustomerAccessGroupId.getId());
                return cagItem;
            });
    }

    public Mono<CustomerAccessGroupItem> getCustomerAccessGroup(StreamTask streamTask, Long cagId) {
        CustomerAccessGroupItem cagItem = new CustomerAccessGroupItem();
        cagItem.setId(cagId);

        return customerAccessGroupApi.getCustomerAccessGroupById(cagId)
            .map(cag -> {
                cagItem.name(cag.getName()).description(cag.getDescription()).mandatory(cag.getMandatory());
                return cagItem;
            });
    }

    public Mono<List<CustomerAccessGroupItem>> getCustomerAccessGroups(StreamTask streamTask) {
        return customerAccessGroupApi.getCustomerAccessGroups(null, null, null)
        .map(response -> {
            return response.getCustomerAccessGroups();
        });
    }

    public Mono<LegalEntityV2> assignCustomerAccessGroupsToLegalEntity(StreamTask streamTask, LegalEntityV2 legalEntity, Set<Long> cagIds) {
        String legalEntityId = legalEntity.getInternalId();

        return customerAccessGroupApi.assignCustomerAccessGroupsToLegalEntity(legalEntityId, cagIds)
        .onErrorResume(WebClientResponseException.class, throwable -> {
            streamTask.error(CUSTOMER_ACCESS_GROUP, "assign", "failed", legalEntity.getExternalId(),
            legalEntity.getInternalId(), throwable, throwable.getResponseBodyAsString(), "Failed to assign CAGs to Legal Entity");
            return Mono.error(new StreamTaskException(streamTask, throwable,
                "Failed to assign CAGs to Legal Entity: " + throwable.getResponseBodyAsString()));
        })
        .then(Mono.just(legalEntity));

    }

    public Mono<ServiceAgreementV2> assignCustomerAccessGroupsToJobRoles(StreamTask streamTask, String userId, ServiceAgreementV2 serviceAgreement, Map<String, Set<Long>> functionGroupsToCags) {

        List<CustomerAccessGroupUserPermissionItem> items = new ArrayList<>();
        for (Entry<String, Set<Long>> fgIdToCagIds : functionGroupsToCags.entrySet()) {
            items.add(new CustomerAccessGroupUserPermissionItem()
                .functionGroupId(fgIdToCagIds.getKey())
                .customerAccessGroupIds(fgIdToCagIds.getValue()));
        }
        return customerAccessGroupApi.assignCustomerAccessGroupsToJobRoles(userId, serviceAgreement.getInternalId(), items)
            .onErrorResume(WebClientResponseException.class, throwable -> {
                // TODO FIX
                streamTask.error(CUSTOMER_ACCESS_GROUP, "assign", "failed", null,
                null, throwable, throwable.getResponseBodyAsString(), "Failed to assign CAGs to Legal Entity");
                return Mono.error(throwable);
            })
            .then(Mono.just(serviceAgreement));

    }
    
}
