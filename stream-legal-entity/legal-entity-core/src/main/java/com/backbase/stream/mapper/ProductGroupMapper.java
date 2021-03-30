package com.backbase.stream.mapper;

import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.ProductGroup;
import org.mapstruct.Mapper;

@Mapper
public interface ProductGroupMapper {

    ProductGroup map(BaseProductGroup baseProductGroup);

}
