package com.backbase.stream.mapper;

import com.backbase.dbs.accesscontrol.api.service.v3.model.FunctionGroupItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ParticipantIngest;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationIngestFunctionGroup;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationPermission;
import com.backbase.dbs.accesscontrol.api.service.v3.model.PresentationPermissionFunctionGroupUpdate;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServiceAgreementItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServiceAgreementItemQuery;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServiceAgreementPut;
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
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface AccessGroupMapper {

    @Mapping(source = "id", target = "internalId")
    ServiceAgreement toStream(ServiceAgreementItemQuery getServiceAgreement);

    @Mapping(source = "id", target = "internalId")
    ServiceAgreementV2 toStreamV2(ServiceAgreementItemQuery getServiceAgreement);

    @Mapping(source = "id", target = "internalId")
    ServiceAgreement toStream(ServiceAgreementItem serviceAgreementItem);

    BusinessFunction toStream(FunctionGroupItem functionsGetResponseBody);

    @Mapping(source = "participants", target = "participantsToIngest")
    ServicesAgreementIngest toPresentation(ServiceAgreement serviceAgreement);

    @Mapping(source = "participants", target = "participantsToIngest")
    ServicesAgreementIngest toPresentation(ServiceAgreementV2 serviceAgreement);

    ServiceAgreementPut toPresentationPut(ServiceAgreement serviceAgreement);

    // Initialize users list to workaround https://backbase.atlassian.net/browse/MAINT-10442
    @Mapping(defaultExpression = "java( new ArrayList<>() )", source = "users", target = "users")
    ParticipantIngest toPresentation(LegalEntityParticipant legalEntityParticipant);

    PresentationIngestFunctionGroup toPresentation(JobRole referenceJobRole);

    @Mapping(source = "serviceAgreementId", target = "internalId")
    @Mapping(source = "externalId", target = "externalId")
    @Mapping(source = "serviceAgreementName", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "purpose", target = "purpose")
    @Mapping(source = "serviceAgreementMaster", target = "isMaster")
    @Mapping(source = "customerCategory", target = "customerCategory")
    ServiceAgreement toStream(UserContextItem userContext);

    List<ServiceAgreement> toStream(List<UserContextItem> userContextItems);

    /**
     * Map {@link BusinessFunctionGroup} with privileges to {@link PresentationPermission}.
     *
     * @param functionGroups defined function groups
     * @return mapped object
     */
    default List<PresentationPermission> toPresentation(List<BusinessFunctionGroup> functionGroups) {
        if (Objects.isNull(functionGroups)) {
            return Collections.emptyList();
        }

        return functionGroups.stream()
            .filter(Objects::nonNull)
            .map(BusinessFunctionGroup::getFunctions)
            .flatMap(Collection::stream)
            .map(f -> {
                PresentationPermission presentationPermission = new PresentationPermission();
                presentationPermission.setFunctionId(f.getFunctionId());
                f.getPrivileges().stream()
                    .filter(Objects::nonNull)
                    .map(Privilege::getPrivilege)
                    .forEach(presentationPermission::addPrivilegesItem);

                return presentationPermission;
            }).collect(Collectors.toList());
    }

    default List<PresentationPermissionFunctionGroupUpdate> toUpdate(List<BusinessFunctionGroup> functionGroups) {
        if (Objects.isNull(functionGroups)) {
            return Collections.emptyList();
        }

        return functionGroups.stream()
            .filter(Objects::nonNull)
            .map(BusinessFunctionGroup::getFunctions)
            .flatMap(Collection::stream)
            .map(f -> {
                PresentationPermissionFunctionGroupUpdate presentationPermission
                    = new PresentationPermissionFunctionGroupUpdate();
                presentationPermission.functionName(f.getName());
                f.getPrivileges().stream()
                    .filter(Objects::nonNull)
                    .map(Privilege::getPrivilege)
                    .forEach(presentationPermission::addPrivilegesItem);

                return presentationPermission;
            }).collect(Collectors.toList());
    }

}
