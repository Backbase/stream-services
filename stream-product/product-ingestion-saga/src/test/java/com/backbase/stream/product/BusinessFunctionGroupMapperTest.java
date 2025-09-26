package com.backbase.stream.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.backbase.accesscontrol.functiongroup.api.service.v1.model.FunctionGroupItem;
import com.backbase.accesscontrol.functiongroup.api.service.v1.model.Permission;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup.TypeEnum;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BusinessFunctionGroupMapperTest {

    private final BusinessFunctionGroupMapper mapper = new BusinessFunctionGroupMapperImpl();

    @Test
    void testMap() {
        // When
        var result = mapper.map(createFunctionGroupItem());

        // Then
        assertNotNull(result);
        assertEquals("1001", result.getId());
        assertEquals("S001", result.getServiceAgreementId());
        assertEquals("Payments", result.getName());
        assertEquals(TypeEnum.DEFAULT, result.getType());
        assertEquals("functionName", result.getFunctions().get(0).getName());
        assertEquals("resourceName", result.getFunctions().get(0).getResourceName());
        assertEquals("CREATE", result.getFunctions().get(0).getPrivileges().get(0).getPrivilege());

    }

    private FunctionGroupItem createFunctionGroupItem() {
        return new FunctionGroupItem()
            .id("1001")
            .serviceAgreementId("S001")
            .name("Payments")
            .type(FunctionGroupItem.TypeEnum.CUSTOM)
            .permissions(Collections.singletonList(createPermission()));
    }

    private Permission createPermission() {
        return new Permission()
            .businessFunctionName("functionName")
            .resourceName("resourceName")
            .privileges(Set.of("CREATE"));
    }

}
