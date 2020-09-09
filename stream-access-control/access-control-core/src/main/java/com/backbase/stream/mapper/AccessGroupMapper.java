package com.backbase.stream.mapper;

import com.backbase.dbs.accesscontrol.query.service.model.SchemaFunctionGroupItem;
import com.backbase.dbs.accesscontrol.query.service.model.ServiceAgreementItem;
import com.backbase.dbs.accessgroup.presentation.service.model.ParticipantIngest;
import com.backbase.dbs.accessgroup.presentation.service.model.PresentationIngestFunctionGroup;
import com.backbase.dbs.accessgroup.presentation.service.model.PresentationPermission;
import com.backbase.dbs.accessgroup.presentation.service.model.ServicesAgreementIngest;
import com.backbase.stream.legalentity.model.BusinessFunction;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.Privilege;
import com.backbase.stream.legalentity.model.ReferenceJobRole;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface AccessGroupMapper {

    @Mapping(source = "id", target = "internalId")
    ServiceAgreement toStream(ServiceAgreementItem getServiceAgreement);

    BusinessFunction toStream(SchemaFunctionGroupItem functionsGetResponseBody);

    @Mapping(source = "participants", target = "participantsToIngest")
    ServicesAgreementIngest toPresentation(ServiceAgreement serviceAgreement);

    // Initialize users list to workaround https://backbase.atlassian.net/browse/MAINT-10442
    @Mapping(defaultExpression = "java( new ArrayList<>() )", source = "users", target = "users")
    ParticipantIngest toPresentation(LegalEntityParticipant legalEntityParticipant);

    @Mapping(target = "type", constant = "TEMPLATE")
    @Mapping(source = "apsId", target = "apsId", defaultValue = "1")
    PresentationIngestFunctionGroup toPresentation(ReferenceJobRole referenceJobRole);

    com.backbase.stream.legalentity.model.ApprovalStatus map(
        com.backbase.dbs.accessgroup.presentation.service.model.ApprovalStatus approvalStatus);


    /**
     * Map {@link BusinessFunctionGroup} with privileges to {@link PresentationPermission}.
     *
     * @param functionGroups defined function groups
     * @return mapped object
     */
    default List<PresentationPermission> toPresentation(List<BusinessFunctionGroup> functionGroups) {
        if (Objects.isNull(functionGroups)) {
            return null;
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

}
