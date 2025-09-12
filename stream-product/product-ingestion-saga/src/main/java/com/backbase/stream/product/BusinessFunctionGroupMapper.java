package com.backbase.stream.product;

import com.backbase.accesscontrol.functiongroup.api.service.v1.model.FunctionGroupItem;
import com.backbase.accesscontrol.functiongroup.api.service.v1.model.FunctionGroupItem.TypeEnum;
import com.backbase.accesscontrol.functiongroup.api.service.v1.model.Permission;
import com.backbase.stream.legalentity.model.BusinessFunction;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.Privilege;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ValueMapping;

@Mapper
public interface BusinessFunctionGroupMapper {

    BusinessFunctionGroup map(FunctionGroupItem functionGroupItem);

    @ValueMapping(source = "CUSTOM", target = "DEFAULT")
    @ValueMapping(source = "SYSTEM", target = "SYSTEM")
    @ValueMapping(source = "REFERENCE", target = "TEMPLATE")
    BusinessFunctionGroup.TypeEnum map(TypeEnum type);

    @Mapping(source = "businessFunctionName", target = "name")
    BusinessFunction map(Permission functionGroupItem);

    @Mapping(target = "privilege", source = "privilege")
    Privilege map(String privilege);

}
