package com.backbase.stream.product;

import com.backbase.dbs.accesscontrol.api.service.v2.model.FunctionGroupItem;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import org.mapstruct.Mapper;

@Mapper
public interface BusinessFunctionGroupMapper {

  BusinessFunctionGroup map(FunctionGroupItem functionGroupItem);

}
