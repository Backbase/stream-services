package com.backbase.stream.product;

import com.backbase.dbs.accesscontrol.api.service.v2.model.FunctionGroupItem;
import com.backbase.dbs.accesscontrol.api.service.v2.model.Permission;
import com.backbase.stream.legalentity.model.BusinessFunction;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface BusinessFunctionGroupMapper {

  @Mapping(source = "permissions", target = "functions")
  BusinessFunctionGroup map(FunctionGroupItem functionGroupItem);

  @Mapping(source = "assignedPrivileges", target = "privileges")
  BusinessFunction mapBusinessFunction(Permission permission);
}
