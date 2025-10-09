package com.backbase.stream.mapper;

import com.backbase.accesscontrol.functiongroup.api.integration.v1.model.FunctionGroupIngest;
import com.backbase.accesscontrol.legalentity.api.service.v1.model.SingleServiceAgreement;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.CustomerCategory;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ParticipantCreateRequest;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ServiceAgreementCreateRequest;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ServiceAgreementDetails;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.Status;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.ServiceAgreementUpdateRequest;
import com.backbase.accesscontrol.usercontext.api.service.v1.model.ContextServiceAgreement;
import com.backbase.stream.legalentity.model.ApsIdentifiers;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.JobRole;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.Privilege;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface AccessGroupMapper {

    @Mapping(source = "id", target = "internalId")
    @Mapping(source = "isSingle", target = "isMaster")
    @Mapping(source = "creatorLegalEntityId", target = "creatorLegalEntity")
    ServiceAgreement toStream(ServiceAgreementDetails getServiceAgreement);

    @Mapping(source = "id", target = "internalId")
    ServiceAgreement toStream(SingleServiceAgreement serviceAgreementItem);

    ServiceAgreementUpdateRequest toPresentationPut(ServiceAgreement serviceAgreement);

    FunctionGroupIngest toPresentation(JobRole referenceJobRole);

    @Mapping(source = "id", target = "internalId")
    @Mapping(source = "isSingle", target = "isMaster")
    ServiceAgreement toStream(ContextServiceAgreement userContext);

    /**
     * @param functionGroups defined function groups
     * @return mapped object
     */
    default List<com.backbase.accesscontrol.functiongroup.api.integration.v1.model.Permission> toPresentation(
        List<BusinessFunctionGroup> functionGroups) {
        if (Objects.isNull(functionGroups)) {
            return Collections.emptyList();
        }

        return functionGroups.stream()
            .filter(Objects::nonNull)
            .map(BusinessFunctionGroup::getFunctions)
            .flatMap(Collection::stream)
            .map(f -> {
                Set<String> privileges = f.getPrivileges().stream()
                    .filter(Objects::nonNull)
                    .map(Privilege::getPrivilege)
                    .collect(Collectors.toSet());
                return new com.backbase.accesscontrol.functiongroup.api.integration.v1.model.Permission()
                    .businessFunctionName(f.getName())
                    .resourceName(f.getResourceName())
                    .privileges(privileges);
            }).collect(Collectors.toList());
    }

    default List<com.backbase.accesscontrol.functiongroup.api.integration.v1.model.Permission> toUpdate(
        List<BusinessFunctionGroup> functionGroups) {
        if (Objects.isNull(functionGroups)) {
            return Collections.emptyList();
        }

        return functionGroups.stream()
            .filter(Objects::nonNull)
            .map(this::toUpdate)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    default List<com.backbase.accesscontrol.functiongroup.api.integration.v1.model.Permission> toUpdate(
        BusinessFunctionGroup functionGroups) {
        if (Objects.isNull(functionGroups)) {
            return null;
        }

        return functionGroups.getFunctions().stream()
            .map(f -> new com.backbase.accesscontrol.functiongroup.api.integration.v1.model.Permission()
                .resourceName(f.getResourceName())
                .businessFunctionName(f.getName())
                .privileges(f.getPrivileges().stream()
                    .filter(Objects::nonNull)
                    .map(Privilege::getPrivilege)
                    .collect(Collectors.toSet())))
            .collect(Collectors.toList());
    }

    default ServiceAgreementCreateRequest map(ServiceAgreement serviceAgreement) {
        return new ServiceAgreementCreateRequest().name(serviceAgreement.getName())
            .description(serviceAgreement.getDescription())
            .status(
                serviceAgreement.getStatus() == null ? Status.ENABLED
                    : Status.valueOf(serviceAgreement.getStatus().toString()))
            .externalId(serviceAgreement.getExternalId())
            .validFrom(serviceAgreement.getValidFrom())
            .validUntil(serviceAgreement.getValidUntil())
            .isSingle(serviceAgreement.getIsMaster())
            .creatorLegalEntityExternalId(serviceAgreement.getCreatorLegalEntity())
            .regularUserApsNames(getNameIdentifiers(serviceAgreement.getRegularUserAps()))
            .adminUserApsNames(getNameIdentifiers(serviceAgreement.getAdminUserAps()))
            .customerCategory(serviceAgreement.getCustomerCategory() == null ? null
                : CustomerCategory.valueOf(serviceAgreement.getCustomerCategory().toString()))
            .purpose(serviceAgreement.getPurpose())
            .participants(mapParticipants(serviceAgreement.getParticipants()))
            .additions(serviceAgreement.getAdditions());
    }

    private static List<String> getNameIdentifiers(ApsIdentifiers aspidentifiers) {
        if (aspidentifiers == null || aspidentifiers.getNameIdentifiers() == null) {
            return Collections.emptyList();
        }
        return aspidentifiers.getNameIdentifiers();
    }

    default List<ParticipantCreateRequest> mapParticipants(List<LegalEntityParticipant> participants) {
        if (participants == null || participants.isEmpty()) {
            return Collections.emptyList();
        }
        return participants.stream().map(participant ->
                new ParticipantCreateRequest()
                    .externalId(participant.getExternalId())
                    .sharingUsers(participant.getSharingUsers())
                    .sharingAccounts(participant.getSharingAccounts())
                    .admins(participant.getAdmins().stream().map(
                        admin -> new com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.Admin().externalUserId(
                            admin)).toList())
                    .users(participant.getUsers().stream()
                        .map(
                            user -> new com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.User().externalUserId(
                                user)).toList()))
            .toList();
    }
}
