package com.backbase.stream.product;

import com.backbase.dbs.accesscontrol.query.service.model.SchemaFunctionGroupItem;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper
public interface BusinessFunctionGroupMapper {

    BusinessFunctionGroup map(SchemaFunctionGroupItem functionGroupItem);

}
