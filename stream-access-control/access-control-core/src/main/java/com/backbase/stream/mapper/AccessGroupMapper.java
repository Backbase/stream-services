package com.backbase.stream.mapper;

import com.backbase.accesscontrol.functiongroup.api.service.v1.model.FunctionGroupCreateRequest;
import com.backbase.accesscontrol.functiongroup.api.service.v1.model.Permission;
import com.backbase.accesscontrol.legalentity.api.service.v1.model.SingleServiceAgreement;
import com.backbase.accesscontrol.serviceagreement.api.integration.v1.model.ServiceAgreementDetails;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.model.ServiceAgreementUpdateRequest;
import com.backbase.accesscontrol.usercontext.api.service.v1.model.ContextServiceAgreement;
import com.backbase.dbs.accesscontrol.api.service.v3.model.FunctionGroupItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ParticipantIngest;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationPermission;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServiceAgreementItemQuery;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServicesAgreementIngest;
import com.backbase.dbs.accesscontrol.api.service.v3.model.UserContextItem;
import com.backbase.stream.legalentity.model.BusinessFunction;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.JobRole;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.Privilege;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.ServiceAgreementV2;
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
    ServiceAgreementV2 toStreamV2(ServiceAgreementItemQuery getServiceAgreement);

    @Mapping(source = "id", target = "internalId")
    ServiceAgreement toStream(SingleServiceAgreement serviceAgreementItem);

    BusinessFunction toStream(FunctionGroupItem functionsGetResponseBody);

    @Mapping(source = "participants", target = "participantsToIngest")
    ServicesAgreementIngest toPresentation(ServiceAgreement serviceAgreement);

    @Mapping(source = "participants", target = "participantsToIngest")
    ServicesAgreementIngest toPresentation(ServiceAgreementV2 serviceAgreement);

    ServiceAgreementUpdateRequest toPresentationPut(ServiceAgreement serviceAgreement);

    // Initialize users list to workaround https://backbase.atlassian.net/browse/MAINT-10442
    @Mapping(defaultExpression = "java( new ArrayList<>() )", source = "users", target = "users")
    ParticipantIngest toPresentation(LegalEntityParticipant legalEntityParticipant);

    FunctionGroupCreateRequest toPresentation(JobRole referenceJobRole);

    @Mapping(source = "id", target = "internalId")
    @Mapping(source = "isSingle", target = "isMaster")
    ServiceAgreement toStream(ContextServiceAgreement userContext);

    List<ServiceAgreement> toStream(List<UserContextItem> userContextItems);

    /**
     * Map {@link BusinessFunctionGroup} with privileges to {@link PresentationPermission}.
     *
     * @param functionGroups defined function groups
     * @return mapped object
     */
    default List<Permission> toPresentation(List<BusinessFunctionGroup> functionGroups) {
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
                return new Permission()
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

}
