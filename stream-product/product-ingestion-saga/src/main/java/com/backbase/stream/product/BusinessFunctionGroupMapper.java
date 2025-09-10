package com.backbase.stream.product;

import com.backbase.accesscontrol.functiongroup.api.service.v1.model.FunctionGroupItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.Permission;
import com.backbase.stream.legalentity.model.BusinessFunction;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.Privilege;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface BusinessFunctionGroupMapper {

    BusinessFunctionGroup map(FunctionGroupItem functionGroupItem);

    @Mapping(source = "businessFunctionName", target = "name")
    BusinessFunction map(com.backbase.accesscontrol.functiongroup.api.service.v1.model.Permission functionGroupItem);

    @Mapping(target = "privilege", expression = "java(privilege)")
    Privilege map(String privilege);

    @Mapping(source = "assignedPrivileges", target = "privileges")
    BusinessFunction mapBusinessFunction(Permission permission);
}
